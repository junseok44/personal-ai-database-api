package com.junseok.personal_data_ai.reminder.condition

import com.junseok.personal_data_ai.notion.ConditionMergeEntry
import com.junseok.personal_data_ai.notion.ConditionNotionService
import org.springframework.stereotype.Service

@Service
class ConditionSyncService(
    private val conditionNotionService: ConditionNotionService,
) {
    fun sync(request: ConditionSyncRequest): ConditionSyncResponse {
        val entries =
            request.conditions.mapNotNull { item ->
                val title = item.title.trim()
                if (title.isBlank()) {
                    null
                } else {
                    ConditionMergeEntry(
                        content = title,
                    )
                }
            }

        val result = conditionNotionService.upsertTodayAndMerge(entries)
        return ConditionSyncResponse(
            pageId = result.pageId,
            mergedCount = result.mergedCount,
            reminderCount = request.conditions.size,
        )
    }
}
