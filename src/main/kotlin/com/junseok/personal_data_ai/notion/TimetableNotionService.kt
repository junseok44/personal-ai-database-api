package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class TimetableNotionService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
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

    private fun findOrCreateTodayPage(): String {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val calendarDate = LocalDate.now(zoneId)
        val todayTitle = calendarDate.format(titleFormatter)
        return notionApiClient.queryDatabaseByTitleEquals(
            databaseId = notionProperties.timetableDatabaseId,
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
            databaseId = notionProperties.timetableDatabaseId,
            properties = titleBlock + dateBlock,
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
}

