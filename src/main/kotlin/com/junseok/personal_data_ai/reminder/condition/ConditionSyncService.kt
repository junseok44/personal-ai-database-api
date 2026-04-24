package com.junseok.personal_data_ai.reminder.condition

import com.junseok.personal_data_ai.notion.ConditionMergeEntry
import com.junseok.personal_data_ai.notion.ConditionNotionService
import com.junseok.personal_data_ai.config.NotionProperties
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ConditionSyncService(
    private val conditionNotionService: ConditionNotionService,
    private val notionProperties: NotionProperties,
) {
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun sync(request: ConditionSyncRequest): ConditionSyncResponse {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val entries =
            request.conditions.mapNotNull { item ->
                val text = item.title.trim()
                if (text.isBlank()) {
                    null
                } else {
                    val hhmm = item.time.atZoneSameInstant(zoneId).toLocalTime().format(timeFormatter)
                    val tag = item.tag?.trim()?.takeIf { it.isNotBlank() }
                    ConditionMergeEntry(
                        hhmm,
                        tag,
                        text,
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
