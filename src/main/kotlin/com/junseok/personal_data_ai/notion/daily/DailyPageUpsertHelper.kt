package com.junseok.personal_data_ai.notion.daily

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionApiClient
import org.springframework.stereotype.Component

@Component
class DailyPageUpsertHelper(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
) {
    /**
     * titleProperty == title 인 페이지를 찾고, 없으면 생성한다.
     *
     * - dateStart: Notion Date 타입 프로퍼티에 넣을 start 값(예: "2026-04-24" 또는 ISO-8601 datetime).
     * - notion.dateProperty가 비어 있으면 dateStart는 무시된다.
     */
    fun findOrCreatePage(
        databaseId: String,
        title: String,
        dateStart: String?,
    ): String {
        val found =
            notionApiClient.queryDatabaseByTitleEquals(
                databaseId = databaseId,
                titleProperty = notionProperties.titleProperty,
                titleEquals = title,
            )
        if (found != null) return found

        val titleBlock =
            mapOf(
                notionProperties.titleProperty to
                    mapOf(
                        "title" to
                            listOf(
                                mapOf(
                                    "type" to "text",
                                    "text" to mapOf("content" to title),
                                ),
                            ),
                    ),
            )

        val dateBlock =
            if (notionProperties.dateProperty.isNotBlank() && !dateStart.isNullOrBlank()) {
                mapOf(
                    notionProperties.dateProperty to
                        mapOf(
                            "date" to mapOf("start" to dateStart),
                        ),
                )
            } else {
                emptyMap()
            }

        return notionApiClient.createPage(
            databaseId = databaseId,
            properties = titleBlock + dateBlock,
        )
    }
}

