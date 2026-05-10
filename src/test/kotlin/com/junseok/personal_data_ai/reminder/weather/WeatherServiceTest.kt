package com.junseok.personal_data_ai.reminder.weather

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionApiClient
import com.junseok.personal_data_ai.notion.NotionBlockContentFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.verify

class WeatherServiceTest {
    private val notionApiClient: NotionApiClient = Mockito.mock(NotionApiClient::class.java)
    private val service =
        WeatherService(
            notionProperties = notionProperties(),
            notionApiClient = notionApiClient,
            notionBlockContentFactory = NotionBlockContentFactory(),
        )

    @Test
    fun `append는 weather title 목록을 지정된 날씨 페이지에 불렛으로 추가한다`() {
        val response =
            service.append(
                WeatherAppendRequest(
                    weather =
                        listOf(
                            WeatherItemRequest(title = "  맑음  "),
                            WeatherItemRequest(title = "바람 강함"),
                        ),
                ),
            )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Map<String, Any>>>
        verify(notionApiClient).appendBlockChildren(Mockito.eq("weather-page-1"), captor.capture() ?: emptyList())

        assertEquals("weather-page-1", response.pageId)
        assertEquals(2, response.appendedCount)
        assertEquals(2, response.weatherCount)
        assertEquals(listOf("맑음", "바람 강함"), captor.value.map { it.bulletedListText() })
    }

    private fun notionProperties(): NotionProperties =
        NotionProperties(
            apiKey = "notion-api-key",
            timetableDatabaseId = "timetable-db",
            tilDatabaseId = "til-db",
            conditionDatabaseId = "condition-db",
            nutritionDatabaseId = "nutrition-db",
            foodDatabaseId = "food-db",
            weatherPageId = "weather-page-1",
        )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.bulletedListText(): String {
        val block = this["bulleted_list_item"] as Map<String, Any>
        val richText = block["rich_text"] as List<Map<String, Any>>
        return richText.joinToString("") { richTextObject ->
            val text = richTextObject["text"] as Map<String, Any>
            text["content"] as String
        }
    }
}
