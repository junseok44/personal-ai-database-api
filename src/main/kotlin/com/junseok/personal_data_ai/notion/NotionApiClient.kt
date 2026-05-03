package com.junseok.personal_data_ai.notion

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NotionApiClient(
    private val notionRestClient: RestClient,
) {
    fun queryDatabaseByTitleEquals(
        databaseId: String,
        titleProperty: String,
        titleEquals: String,
    ): String? {
        val requestBody =
            mapOf(
                "filter" to
                    mapOf(
                        "property" to titleProperty,
                        "title" to mapOf("equals" to titleEquals),
                    ),
                "page_size" to 1,
            )
        val response =
            notionRestClient
                .post()
                .uri("/databases/{databaseId}/query", databaseId)
                .body(requestBody)
                .retrieve()
                .body(NotionQueryResponse::class.java)
                ?: return null
        return response.results.firstOrNull()?.id?.takeIf { it.isNotBlank() }
    }

    fun createPage(
        databaseId: String,
        properties: Map<String, Any>,
    ): String {
        val requestBody =
            mapOf(
                "parent" to mapOf("database_id" to databaseId),
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

    fun patchPageProperties(
        pageId: String,
        properties: Map<String, Any>,
    ) {
        notionRestClient
            .patch()
            .uri("/pages/{pageId}", pageId)
            .body(mapOf("properties" to properties))
            .retrieve()
            .toBodilessEntity()
    }

    fun archivePage(pageId: String) {
        notionRestClient
            .patch()
            .uri("/pages/{pageId}", pageId)
            .body(mapOf("archived" to true))
            .retrieve()
            .toBodilessEntity()
    }

    fun appendBlockChildren(
        blockId: String,
        children: List<Map<String, Any>>,
    ): List<NotionBlock> {
        val response =
            notionRestClient
            .patch()
            .uri("/blocks/{blockId}/children", blockId)
            .body(mapOf("children" to children))
            .retrieve()
            .body(NotionBlockChildrenResponse::class.java)
            ?: throw IllegalStateException("Notion block append failed: empty response")
        return response.results
    }

    fun updateBulletedListItemText(
        blockId: String,
        text: String,
    ) {
        val requestBody =
            mapOf(
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
        notionRestClient
            .patch()
            .uri("/blocks/{blockId}", blockId)
            .body(requestBody)
            .retrieve()
            .toBodilessEntity()
    }

    fun fetchAllBlockChildren(blockId: String): List<NotionBlock> {
        val all = mutableListOf<NotionBlock>()
        var nextCursor: String? = null
        do {
            val cursor = nextCursor
            val response =
                notionRestClient.get()
                    ?.uri { builder ->
                        val uriBuilder =
                            builder
                                .path("/blocks/{blockId}/children")
                                .queryParam("page_size", 100)
                        if (cursor != null) {
                            uriBuilder.queryParam("start_cursor", cursor)
                        }
                        uriBuilder.build(blockId)
                    }?.retrieve()
                    ?.body(NotionBlockChildrenResponse::class.java)
                    ?: break
            all.addAll(response.results)
            nextCursor = if (response.has_more) response.next_cursor else null
        } while (nextCursor != null)
        return all
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionQueryResponse(
    val results: List<NotionPageResponse> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionPageResponse(
    val id: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionBlock(
    val id: String = "",
    val type: String = "",
    val has_children: Boolean = false,
    val bulleted_list_item: NotionBulletedListItem? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionBulletedListItem(
    val rich_text: List<NotionRichText> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotionRichText(
    val plain_text: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class NotionBlockChildrenResponse(
    val results: List<NotionBlock> = emptyList(),
    val has_more: Boolean = false,
    val next_cursor: String? = null,
)

