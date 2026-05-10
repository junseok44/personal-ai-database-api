package com.junseok.personal_data_ai.reminder.weather

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionApiClient
import com.junseok.personal_data_ai.notion.NotionBlockContentFactory
import org.springframework.stereotype.Service

@Service
class WeatherService(
    private val notionProperties: NotionProperties,
    private val notionApiClient: NotionApiClient,
    private val notionBlockContentFactory: NotionBlockContentFactory,
) {
    fun append(request: WeatherAppendRequest): WeatherAppendResponse {
        val pageId = notionProperties.weatherPageId.trim()
        require(pageId.isNotBlank()) { "notion.weather-page-id must be configured" }

        val titles = request.weather.mapNotNull { item -> item.title.trim().takeIf { it.isNotBlank() } }
        if (titles.isNotEmpty()) {
            notionApiClient.appendBlockChildren(
                pageId,
                titles.map { notionBlockContentFactory.createBulletedBlock(it) },
            )
        }

        return WeatherAppendResponse(
            pageId = pageId,
            appendedCount = titles.size,
            weatherCount = request.weather.size,
        )
    }
}
