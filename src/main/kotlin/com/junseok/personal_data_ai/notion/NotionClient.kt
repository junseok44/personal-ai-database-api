package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class NotionClient(
    private val notionRestClient: RestClient,
    private val notionProperties: NotionProperties,
) {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    fun upsertTodayPageAndUpdateProperties(categoryBullets: Map<String, String>): String {
        val todayTitle = LocalDate.now().format(dateFormatter)
        val pageId = findTodayPageId(todayTitle) ?: createPage(todayTitle)
        updatePageProperties(pageId, categoryBullets)
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

    private fun createPage(todayTitle: String): String {
        val requestBody =
            mapOf(
                "parent" to mapOf("database_id" to notionProperties.databaseId),
                "properties" to
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
                    ),
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
        categoryBullets: Map<String, String>,
    ) {

        var allowedColumns = listOf("TIL", "한 일", "회고", "피드백", "내일 할 것", "TIL Shorts")

        var filteredBullets = categoryBullets.filterKeys { it in allowedColumns }
        
        val properties =
            filteredBullets.mapValues { (_, bulletText) ->
                mapOf(
                    "rich_text" to
                        listOf(
                            mapOf(
                                "type" to "text",
                                "text" to mapOf("content" to bulletText),
                            ),
                        ),
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
}

private data class NotionQueryResponse(
    val results: List<NotionPageResponse> = emptyList(),
)

private data class NotionPageResponse(
    val id: String = "",
)
