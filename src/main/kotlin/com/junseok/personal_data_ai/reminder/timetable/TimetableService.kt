package com.junseok.personal_data_ai.reminder.timetable

import com.junseok.personal_data_ai.config.ReminderProperties
import com.junseok.personal_data_ai.llm.AiGenerateService
import com.junseok.personal_data_ai.notion.TimetableThinkingGroup
import com.junseok.personal_data_ai.notion.NotionPagePropertyContent
import com.junseok.personal_data_ai.notion.TimetableNotionService
import org.slf4j.LoggerFactory
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service

@Service
class TimetableService(
    private val timetableNotionService: TimetableNotionService,
    private val reminderProperties: ReminderProperties,
    private val timetableThinkingAiFeedbackService: TimetableThinkingAiFeedbackService,
) {
    fun sync(request: TimetableSyncRequest): TimetableSyncResponse {
        val allowedCategorySet =
            reminderProperties.allowedCategories
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()

        val groupedByCategory =
            request.reminders
                .groupBy { it.category.trim() }
                .mapValues { (category, items) ->
                    val filtered = items.filter { it.title.trim().isNotBlank() }
                    if (filtered.isEmpty()) {
                        NotionPagePropertyContent.Plain("")
                    } else if (category == "한 일") {
                        NotionPagePropertyContent.Rich(DoneTodosReminderFormatter.formatRichText(filtered))
                    } else {
                        NotionPagePropertyContent.Plain(formatOtherCategories(filtered))
                    }
                }.filterValues { content ->
                    when (content) {
                        is NotionPagePropertyContent.Plain -> content.text.isNotBlank()
                        is NotionPagePropertyContent.Rich -> content.parts.isNotEmpty()
                    }
                }
                .filterKeys { category ->
                    allowedCategorySet.isEmpty() || allowedCategorySet.contains(category)
                }

        val pageId = timetableNotionService.upsertTodayAndUpdateProperties(groupedByCategory)
        return TimetableSyncResponse(
            pageId = pageId,
            categoryCount = groupedByCategory.size,
            reminderCount = request.reminders.size,
        )
    }

    fun appendThinking(request: TimetableThinkingAppendRequest): TimetableThinkingAppendResponse {
        val groupedByKey = linkedMapOf<String, TimetableThinkingGroupBuilder>()
        var noTagIndex = 0
        for (item in request.thinking) {
            val title = item.title.trim()
            if (title.isBlank()) continue

            val tags = item.tags.map { it.trim() }.filter { it.isNotBlank() }
            val groupingTags = tags.sorted()
            val key =
                if (groupingTags.isEmpty()) {
                    // 태그가 없으면 묶지 않고 각각 독립 불렛(=독립 그룹)로 처리
                    "__NO_TAG__#${noTagIndex++}"
                } else {
                    groupingTags.joinToString(separator = ",")
                }

            val builder = groupedByKey.getOrPut(key) { TimetableThinkingGroupBuilder() }
            builder.items.add(title)
        }

        val groups =
            groupedByKey.values
                .mapNotNull { builder ->
                    val items = builder.items.map { it.trim() }.filter { it.isNotBlank() }
                    if (items.isEmpty()) return@mapNotNull null

                    TimetableThinkingGroup(
                        title = items.first(),
                        children = items.drop(1),
                    )
                }

        val result = timetableNotionService.upsertTodayAndAppendThinking(groups)
        timetableThinkingAiFeedbackService.appendFeedbackAsync(
            groups = groups,
            blockIds = result.appendedTopLevelBlockIds,
        )
        return TimetableThinkingAppendResponse(
            pageId = result.pageId,
            tagCount = groupedByKey.keys.count { !it.startsWith("__NO_TAG__#") },
            appendedTopLevelBulletCount = result.appendedTopLevelBulletCount,
            thinkingCount = request.thinking.size,
        )
    }

    private fun formatOtherCategories(items: List<TimetableItemRequest>): String =
        items.joinToString(separator = "\n") { item -> "• ${item.title.trim()}" }
}

private data class TimetableThinkingGroupBuilder(
    val items: MutableList<String> = mutableListOf(),
)

@Service
class TimetableThinkingAiFeedbackService(
    private val aiGenerateService: AiGenerateService,
    private val timetableNotionService: TimetableNotionService,
    private val taskExecutor: TaskExecutor,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun appendFeedbackAsync(
        groups: List<TimetableThinkingGroup>,
        blockIds: List<String>,
    ) {
        groups.zip(blockIds)
            .filter { (_, blockId) -> blockId.isNotBlank() }
            .forEach { (group, blockId) ->
                taskExecutor.execute {
                    appendFeedback(group, blockId)
                }
            }
    }

    private fun appendFeedback(
        group: TimetableThinkingGroup,
        blockId: String,
    ) {
        runCatching {
            val aiText = aiGenerateService.generateText(prompt = buildAiPrompt(group.items()))
            timetableNotionService.appendAiFeedbackToThinkingBlock(blockId, aiText)
        }.onFailure { error ->
            logger.warn("Failed to append timetable thinking AI feedback. blockId={}", blockId, error)
        }
    }

    private fun TimetableThinkingGroup.items(): List<String> = listOf(title) + children
}

private fun buildAiPrompt(items: List<String>): String {
    val lines = items.map { it.trim() }.filter { it.isNotBlank() }
    return buildString {
        appendLine("다음은 사용자의 생각 목록입니다. 듣고 조언을 해줘.")
        appendLine()
        for (line in lines) {
            appendLine("- $line")
        }
    }.trim()
}

