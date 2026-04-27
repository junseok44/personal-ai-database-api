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

    fun upsertTodayAndAppendThinking(groupedThinking: List<TimetableThinkingGroup>): TimetableThinkingAppendResult {
        val pageId = findOrCreateTodayPage()
        val blocks = buildThinkingBlocks(groupedThinking)
        if (blocks.isNotEmpty()) {
            notionApiClient.appendBlockChildren(pageId, blocks)
        }
        return TimetableThinkingAppendResult(
            pageId = pageId,
            appendedTopLevelBulletCount = groupedThinking.size,
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

    private fun buildThinkingBlocks(groupedThinking: List<TimetableThinkingGroup>): List<Map<String, Any>> {
        val blocks = mutableListOf<Map<String, Any>>()
        for (group in groupedThinking) {
            val first = group.title.trim()
            if (first.isBlank()) continue

            val children = mutableListOf<Map<String, Any>>()
            children.addAll(
                group.children
                    .mapNotNull { it.trim().takeIf { t -> t.isNotBlank() } }
                    .map { createBulletedBlock(it) },
            )

            val feedback = group.aiFeedback?.trim().orEmpty()
            if (feedback.isNotBlank()) {
                children.add(
                    createToggleBlock(
                        "AI의 피드백",
                        children = listOf(createParagraphBlock(feedback)),
                    ),
                )
            }

            blocks.add(createBulletedBlock(first, children))
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

    private fun createToggleBlock(
        text: String,
        children: List<Map<String, Any>> = emptyList(),
    ): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "toggle",
            "toggle" to
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

    private fun createParagraphBlock(text: String): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "paragraph",
            "paragraph" to
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
}

data class TimetableThinkingAppendResult(
    val pageId: String,
    val appendedTopLevelBulletCount: Int,
)


