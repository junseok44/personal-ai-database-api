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
                .groupBy(
                    keySelector = { it.category.trim() },
                    valueTransform = { it.title.trim() },
                ).mapValues { (_, titles) ->
                    titles
                        .filter { it.isNotBlank() }
                        .joinToString(separator = "\n") { "• $it" }
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
}
