package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.daily.DailyPageUpsertHelper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class TimetableNotionService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
    private val dailyPageUpsertHelper: DailyPageUpsertHelper,
) {
    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    fun upsertTodayAndUpdateProperties(categoryContent: Map<String, NotionPagePropertyContent>): String {
        val pageId = findOrCreateTodayPage()

        val properties =
            categoryContent.mapValues { (_, content) ->
                mapOf(
                    "rich_text" to richTextArray(content),
                )
            }

        if (properties.isNotEmpty()) {
            notionApiClient.patchPageProperties(pageId, properties)
        }
        return pageId
    }

    fun upsertTodayAndAppendThinkingBullets(groupedThinking: List<List<String>>): TimetableThinkingAppendResult {
        val pageId = findOrCreateTodayPage()
        val blocks = buildThinkingBulletedBlocks(groupedThinking)
        if (blocks.isNotEmpty()) {
            notionApiClient.appendBlockChildren(pageId, blocks)
        }
        return TimetableThinkingAppendResult(
            pageId = pageId,
            appendedTopLevelBulletCount = groupedThinking.count { it.isNotEmpty() },
        )
    }

    private fun findOrCreateTodayPage(): String {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val calendarDate = LocalDate.now(zoneId)
        val todayTitle = calendarDate.format(titleFormatter)
        val dateStart = if (notionProperties.dateProperty.isNotBlank()) calendarDate.toString() else null
        return dailyPageUpsertHelper.findOrCreatePage(
            databaseId = notionProperties.timetableDatabaseId,
            title = todayTitle,
            dateStart = dateStart,
        )
    }

    private fun richTextArray(content: NotionPagePropertyContent): List<Map<String, Any>> =
        when (content) {
            is NotionPagePropertyContent.Plain ->
                listOf(richTextObject(content.text, "default"))
            is NotionPagePropertyContent.Rich ->
                content.parts.map { richTextObject(it.text, it.color) }
        }

    private fun richTextObject(
        text: String,
        color: String,
    ): Map<String, Any> =
        mapOf(
            "type" to "text",
            "text" to mapOf("content" to text),
            "annotations" to
                mapOf(
                    "bold" to false,
                    "italic" to false,
                    "strikethrough" to false,
                    "underline" to false,
                    "code" to false,
                    "color" to color,
                ),
        )

    private fun buildThinkingBulletedBlocks(groupedThinking: List<List<String>>): List<Map<String, Any>> {
        val blocks = mutableListOf<Map<String, Any>>()
        for (group in groupedThinking) {
            if (group.isEmpty()) continue
            val first = group.first().trim()
            if (first.isBlank()) continue

            val children =
                group.drop(1)
                    .mapNotNull { it.trim().takeIf { t -> t.isNotBlank() } }
                    .map { createBulletedBlock(it) }

            blocks.add(
                if (children.isEmpty()) {
                    createBulletedBlock(first)
                } else {
                    createBulletedBlock(first, children)
                },
            )
        }
        return blocks
    }

    private fun createBulletedBlock(
        text: String,
        children: List<Map<String, Any>> = emptyList(),
    ): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "bulleted_list_item",
            "bulleted_list_item" to
                buildMap<String, Any> {
                    put(
                        "rich_text",
                        listOf(
                            mapOf(
                                "type" to "text",
                                "text" to mapOf("content" to text),
                            ),
                        ),
                    )
                    if (children.isNotEmpty()) {
                        put("children", children)
                    }
                },
        )
}

data class TimetableThinkingAppendResult(
    val pageId: String,
    val appendedTopLevelBulletCount: Int,
)


