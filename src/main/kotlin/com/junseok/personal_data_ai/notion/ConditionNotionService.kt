package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ConditionNotionService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
) {
    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    fun upsertTodayAndMerge(entries: List<ConditionMergeEntry>): ConditionMergeResult {
        val pageId = findOrCreateTodayPage()
        val mergedCount = mergeBullets(pageId, entries)
        return ConditionMergeResult(
            pageId = pageId,
            mergedCount = mergedCount,
        )
    }

    private fun findOrCreateTodayPage(): String {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val calendarDate = LocalDate.now(zoneId)
        val todayTitle = calendarDate.format(titleFormatter)
        return notionApiClient.queryDatabaseByTitleEquals(
            databaseId = notionProperties.conditionDatabaseId,
            titleProperty = notionProperties.titleProperty,
            titleEquals = todayTitle,
        ) ?: createTodayPage(todayTitle, calendarDate)
    }

    private fun createTodayPage(
        todayTitle: String,
        calendarDate: LocalDate,
    ): String {
        val titleBlock =
            mapOf(
                notionProperties.titleProperty to
                    mapOf(
                        "title" to
                            listOf(
                                mapOf(
                                    "type" to "text",
                                    "text" to mapOf("content" to todayTitle),
                                ),
                            ),
                    ),
            )
        val dateBlock =
            if (notionProperties.dateProperty.isNotBlank()) {
                mapOf(
                    notionProperties.dateProperty to
                        mapOf(
                            "date" to mapOf("start" to calendarDate.toString()),
                        ),
                )
            } else {
                emptyMap()
            }
        return notionApiClient.createPage(
            databaseId = notionProperties.conditionDatabaseId,
            properties = titleBlock + dateBlock,
        )
    }

    private fun mergeBullets(
        pageId: String,
        entries: List<ConditionMergeEntry>,
    ): Int {
        if (entries.isEmpty()) return 0

        val existingTopBlocks = notionApiClient.fetchAllBlockChildren(pageId)
        val existingPlainBullets = linkedSetOf<String>()
        for (block in existingTopBlocks) {
            if (block.type != "bulleted_list_item") continue
            val text = block.bulleted_list_item.toPlainText().trim()
            if (text.isBlank()) continue
            existingPlainBullets.add(text)
        }

        val appendBlocks = mutableListOf<Map<String, Any>>()
        var mergedCount = 0
        for (entry in entries) {
            val text = entry.content.trim()
            if (text.isBlank()) continue
            if (existingPlainBullets.add(text)) {
                mergedCount += 1
                appendBlocks.add(createBulletedBlock(text))
            }
        }

        if (appendBlocks.isNotEmpty()) {
            notionApiClient.appendBlockChildren(pageId, appendBlocks)
        }
        return mergedCount
    }

    private fun createBulletedBlock(text: String): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "bulleted_list_item",
            "bulleted_list_item" to
                mapOf(
                    "rich_text" to
                        listOf(
                            mapOf(
                                "type" to "text",
                                "text" to mapOf("content" to text),
                            ),
                        ),
                ),
        )

    private fun NotionBulletedListItem?.toPlainText(): String =
        this?.rich_text?.joinToString("") { it.plain_text } ?: ""
}

data class ConditionMergeEntry(
    val content: String,
)

data class ConditionMergeResult(
    val pageId: String,
    val mergedCount: Int,
)

