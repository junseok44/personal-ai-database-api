package com.junseok.personal_data_ai.reminder

import com.junseok.personal_data_ai.config.ReminderProperties
import com.junseok.personal_data_ai.notion.NotionClient
import org.springframework.stereotype.Service

@Service
class ReminderSyncService(
    private val notionClient: NotionClient,
    private val reminderProperties: ReminderProperties,
) {
    fun sync(request: ReminderSyncRequest): ReminderSyncResponse {
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
                        ""
                    } else if (category == "한 일") {
                        DoneTodosReminderFormatter.format(filtered)
                    } else {
                        formatOtherCategories(filtered)
                    }
                }.filterValues { it.isNotBlank() }
                .filterKeys { category ->
                    allowedCategorySet.isEmpty() || allowedCategorySet.contains(category)
                }

        val pageId = notionClient.upsertTodayPageAndUpdateProperties(groupedByCategory)
        return ReminderSyncResponse(
            pageId = pageId,
            categoryCount = groupedByCategory.size,
            reminderCount = request.reminders.size,
        )
    }

    private fun formatOtherCategories(items: List<ReminderItemRequest>): String =
        items.joinToString(separator = "\n") { item -> "• ${item.title.trim()}" }
}
