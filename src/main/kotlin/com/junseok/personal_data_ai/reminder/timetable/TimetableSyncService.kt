package com.junseok.personal_data_ai.reminder.timetable

import com.junseok.personal_data_ai.config.ReminderProperties
import com.junseok.personal_data_ai.notion.NotionPagePropertyContent
import com.junseok.personal_data_ai.notion.TimetableNotionService
import org.springframework.stereotype.Service

@Service
class TimetableSyncService(
    private val timetableNotionService: TimetableNotionService,
    private val reminderProperties: ReminderProperties,
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
        val groupedByKey = linkedMapOf<String, MutableList<String>>()
        var noTagIndex = 0
        for (item in request.thinking) {
            val title = item.title.trim()
            if (title.isBlank()) continue

            val tag = item.tag?.trim().orEmpty()
            val key =
                if (tag.isBlank()) {
                    // 태그가 없으면 묶지 않고 각각 독립 불렛(=독립 그룹)로 처리
                    "__NO_TAG__#${noTagIndex++}"
                } else {
                    tag
                }

            groupedByKey.getOrPut(key) { mutableListOf() }.add(title)
        }

        val groupedThinking = groupedByKey.values.map { it.toList() }
        val result = timetableNotionService.upsertTodayAndAppendThinkingBullets(groupedThinking)
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

