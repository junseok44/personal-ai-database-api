package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class NotionClient(
    private val notionRestClient: RestClient,
    private val notionProperties: NotionProperties,
) {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")
    private val dayZoneId: ZoneId = ZoneId.of(notionProperties.dayTimeZone)

    fun upsertTodayPageAndUpdateProperties(categoryContent: Map<String, NotionPagePropertyContent>): String {
        val calendarDate = LocalDate.now(dayZoneId)
        val todayTitle = calendarDate.format(dateFormatter)
        val pageId = findTodayPageId(todayTitle) ?: createPage(todayTitle, calendarDate)
        updatePageProperties(pageId, categoryContent)
        return pageId
    }

    private fun findTodayPageId(todayTitle: String): String? {
        val requestBody =
            mapOf(
                "filter" to
                    mapOf(
                        "property" to notionProperties.titleProperty,
                        "title" to mapOf("equals" to todayTitle),
                    ),
                "page_size" to 1,
            )

        val response =
            notionRestClient
                .post()
                .uri("/databases/{databaseId}/query", notionProperties.databaseId)
                .body(requestBody)
                .retrieve()
                .body(NotionQueryResponse::class.java)
                ?: return null

        return response.results.firstOrNull()?.id?.takeIf { it.isNotBlank() }
    }

    private fun createPage(
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
                            "date" to
                                mapOf(
                                    "start" to calendarDate.toString(),
                                ),
                        ),
                )
            } else {
                emptyMap()
            }
        val properties = titleBlock + dateBlock

        val requestBody =
            mapOf(
                "parent" to mapOf("database_id" to notionProperties.databaseId),
                "properties" to properties,
            )

        val response =
            notionRestClient
                .post()
                .uri("/pages")
                .body(requestBody)
                .retrieve()
                .body(NotionPageResponse::class.java)
                ?: throw IllegalStateException("Notion page creation failed: empty response")

        val pageId = response.id
        require(pageId.isNotBlank()) { "Notion page creation failed: missing page id" }
        return pageId
    }

    private fun updatePageProperties(
        pageId: String,
        categoryContent: Map<String, NotionPagePropertyContent>,
    ) {
        var allowedColumns = listOf("TIL", "한 일", "회고", "피드백", "내일 할 것", "TIL Shorts")

        var filteredContent = categoryContent.filterKeys { it in allowedColumns }

        val properties =
            filteredContent.mapValues { (_, content) ->
                mapOf(
                    "rich_text" to richTextArray(content),
                )
            }

        val requestBody = mapOf("properties" to properties)
        notionRestClient
            .patch()
            .uri("/pages/{pageId}", pageId)
            .body(requestBody)
            .retrieve()
            .toBodilessEntity()
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

private data class NotionQueryResponse(
    val results: List<NotionPageResponse> = emptyList(),
)

private data class NotionPageResponse(
    val id: String = "",
)
