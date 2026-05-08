package com.junseok.personal_data_ai.reminder.timetable

import com.junseok.personal_data_ai.config.ReminderProperties
import com.junseok.personal_data_ai.config.SlackProperties
import com.junseok.personal_data_ai.llm.AiGenerateService
import com.junseok.personal_data_ai.notion.TimetableThinkingGroup
import com.junseok.personal_data_ai.notion.NotionPagePropertyContent
import com.junseok.personal_data_ai.notion.TimetableNotionService
import com.junseok.personal_data_ai.slack.SlackApiClient
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory
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
    private val slackApiClient: SlackApiClient,
    private val slackProperties: SlackProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val feedbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun appendFeedbackAsync(
        groups: List<TimetableThinkingGroup>,
        blockIds: List<String>,
    ) {
        groups.zip(blockIds)
            .filter { (_, blockId) -> blockId.isNotBlank() }
            .map { (group, blockId) ->
                feedbackScope.launch {
                    appendFeedback(group, blockId)
                }
            }
    }

    @PreDestroy
    fun destroy() {
        feedbackScope.cancel()
    }

    private fun appendFeedback(
        group: TimetableThinkingGroup,
        blockId: String,
    ) {
        runCatching {
            val aiText = aiGenerateService.generateText(prompt = buildAiPrompt(group.items()))
            slackApiClient.postMessage(
                channelId = slackProperties.timetableFeedbackChannelId,
                text = buildSlackMessage(group, aiText),
            )
        }.onFailure { error ->
            logger.warn("Failed to send timetable thinking AI feedback to Slack. blockId={}", blockId, error)
        }
    }

    private fun TimetableThinkingGroup.items(): List<String> = thinkingItems(this)
}

private fun buildSlackMessage(
    group: TimetableThinkingGroup,
    aiText: String,
): String =
    buildString {
        appendLine("*생각 피드백*")
        appendLine()
        appendLine("*생각*")
        for (item in thinkingItems(group)) {
            appendLine("- $item")
        }
        appendLine()
        appendLine("*AI 피드백*")
        append(aiText.trim())
    }

private fun thinkingItems(group: TimetableThinkingGroup): List<String> = listOf(group.title) + group.children

private fun buildAiPrompt(items: List<String>): String {
    val lines = items.map { it.trim() }.filter { it.isNotBlank() }
    return buildString {
        appendLine("다음은 사용자의 생각 목록입니다. 듣고 조언을 해줘. 응답은 마크다운 형식을 빼고 그냥 구조화된 글 형태로 써줘.")
        appendLine()
        for (line in lines) {
            appendLine("- $line")
        }
    }.trim()
}

