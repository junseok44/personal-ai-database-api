package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.daily.DailyPageUpsertHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ConditionNotionServiceTest {
    private val notionApiClient: NotionApiClient = Mockito.mock(NotionApiClient::class.java)
    private val dailyPageUpsertHelper: DailyPageUpsertHelper = Mockito.mock(DailyPageUpsertHelper::class.java)
    private val notionBlockContentFactory = NotionBlockContentFactory()

    private val notionProperties =
        NotionProperties(
            apiKey = "test",
            timetableDatabaseId = "timetable-db",
            tilDatabaseId = "til-db",
            conditionDatabaseId = "condition-db",
            nutritionDatabaseId = "nutrition-db",
            foodDatabaseId = "food-db",
            dateProperty = "Date",
        )

    @Test
    fun `upsertTodayAndMerge는 기존 시간대는 중복 제거 후 추가하고 신규 시간대는 헤더를 생성한다`() {
        val service =
            ConditionNotionService(
                notionApiClient = notionApiClient,
                notionProperties = notionProperties,
                dailyPageUpsertHelper = dailyPageUpsertHelper,
                notionBlockContentFactory = notionBlockContentFactory,
            )

        val today = LocalDate.now(ZoneId.of(notionProperties.dayTimeZone))
        val todayTitle = today.format(DateTimeFormatter.ofPattern("MMdd"))
        val expectedDateStart = today.toString()

        Mockito.`when`(
            dailyPageUpsertHelper.findOrCreatePage(
                notionProperties.conditionDatabaseId,
                todayTitle,
                expectedDateStart,
            ),
        ).thenReturn("page-1")

        // top-level: 08:00 헤더가 이미 있고, 자식에 "물 마시기"가 있음 → "물 마시기"는 중복으로 append 안 됨
        Mockito.`when`(notionApiClient.fetchAllBlockChildren("page-1")).thenReturn(
            listOf(
                NotionBlock(
                    id = "h1",
                    type = "bulleted_list_item",
                    bulleted_list_item =
                        NotionBulletedListItem(
                            rich_text = listOf(NotionRichText(plain_text = "08:00")),
                        ),
                ),
            ),
        )

        Mockito.`when`(notionApiClient.fetchAllBlockChildren("h1")).thenReturn(
            listOf(
                NotionBlock(
                    id = "c1",
                    type = "bulleted_list_item",
                    bulleted_list_item =
                        NotionBulletedListItem(
                            rich_text = listOf(NotionRichText(plain_text = "물 마시기")),
                        ),
                ),
            ),
        )

        val result =
            service.upsertTodayAndMerge(
                listOf(
                    ConditionMergeEntry(time = "08:00", tag = "집중", text = "물 마시기"),
                    ConditionMergeEntry(time = "08:00", tag = null, text = "스트레칭"),
                    ConditionMergeEntry(time = "09:00", tag = "휴식", text = "산책"),
                ),
            )

        verify(notionApiClient).updateBulletedListItemText("h1", "08:00 / 집중")
        verify(notionApiClient).appendBlockChildren(
            "h1",
            listOf(notionBlockContentFactory.createBulletedBlock("스트레칭")),
        )
        verify(notionApiClient).appendBlockChildren(
            "page-1",
            listOf(
                notionBlockContentFactory.createBulletedBlock(
                    "09:00 / 휴식",
                    children = listOf(notionBlockContentFactory.createBulletedBlock("산책")),
                ),
            ),
        )

        assertEquals("page-1", result.pageId)
        assertEquals(2, result.mergedCount)
    }
}
