package com.junseok.personal_data_ai.reminder.timetable

import com.junseok.personal_data_ai.config.ReminderProperties
import com.junseok.personal_data_ai.config.SlackProperties
import com.junseok.personal_data_ai.llm.AiGenerateService
import com.junseok.personal_data_ai.notion.NotionPagePropertyContent
import com.junseok.personal_data_ai.notion.TimetableNotionService
import com.junseok.personal_data_ai.notion.TimetableThinkingAppendResult
import com.junseok.personal_data_ai.notion.TimetableThinkingGroup
import com.junseok.personal_data_ai.slack.SlackApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class TimetableServiceTest {
    private val timetableNotionService: TimetableNotionService = Mockito.mock(TimetableNotionService::class.java)
    private val timetableThinkingAiFeedbackService: TimetableThinkingAiFeedbackService =
        Mockito.mock(TimetableThinkingAiFeedbackService::class.java)

    @Test
    fun `sync는 허용 카테고리만 노션 프로퍼티로 반영한다`() {
        val service =
            TimetableService(
                timetableNotionService = timetableNotionService,
                reminderProperties = ReminderProperties(allowedCategories = listOf("계획", "한 일")),
                timetableThinkingAiFeedbackService = timetableThinkingAiFeedbackService,
            )
        Mockito.`when`(timetableNotionService.upsertTodayAndUpdateProperties(Mockito.anyMap())).thenReturn("page-1")

        val response =
            service.sync(
                TimetableSyncRequest(
                    reminders =
                        listOf(
                            TimetableItemRequest(title = "  운동  ", category = "계획"),
                            TimetableItemRequest(title = "   ", category = "계획"),
                            TimetableItemRequest(title = "작업 완료", category = "한 일", tag = 30),
                            TimetableItemRequest(title = "기록 완료", category = "한 일"),
                            TimetableItemRequest(title = "잡담", category = "기타"),
                        ),
                ),
            )

        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, NotionPagePropertyContent>>
        verify(timetableNotionService).upsertTodayAndUpdateProperties(captor.capture() ?: emptyMap())
        val grouped = captor.value

        assertEquals("page-1", response.pageId)
        assertEquals(2, response.categoryCount)
        assertEquals(5, response.reminderCount)
        assertEquals(setOf("계획", "한 일"), grouped.keys)

        val planContent = grouped["계획"] as NotionPagePropertyContent.Plain
        assertEquals("• 운동", planContent.text)

        val doneContent = grouped["한 일"] as NotionPagePropertyContent.Rich
        assertTrue(doneContent.parts.isNotEmpty())
    }

    @Test
    fun `appendThinking은 태그 기준으로 그룹핑해 먼저 노션에 저장하고 AI 피드백은 비동기로 요청한다`() {
        val service =
            TimetableService(
                timetableNotionService = timetableNotionService,
                reminderProperties = ReminderProperties(),
                timetableThinkingAiFeedbackService = timetableThinkingAiFeedbackService,
            )
        Mockito.`when`(timetableNotionService.upsertTodayAndAppendThinking(Mockito.anyList())).thenReturn(
            TimetableThinkingAppendResult(
                pageId = "page-2",
                appendedTopLevelBulletCount = 3,
                appendedTopLevelBlockIds = listOf("block-1", "block-2", "block-3"),
            ),
        )

        val response =
            service.appendThinking(
                TimetableThinkingAppendRequest(
                    thinking =
                        listOf(
                            TimetableThinkingItemRequest(title = "첫 생각", tags = listOf("work")),
                            TimetableThinkingItemRequest(title = "같은 그룹 자식", tags = listOf("work")),
                            TimetableThinkingItemRequest(title = "무태그 1", tags = emptyList()),
                            TimetableThinkingItemRequest(title = "무태그 2", tags = emptyList()),
                        ),
                ),
            )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<TimetableThinkingGroup>>
        verify(timetableNotionService).upsertTodayAndAppendThinking(captor.capture() ?: emptyList())

        val groups = captor.value
        assertEquals(3, groups.size)
        assertEquals("첫 생각", groups[0].title)
        assertEquals(listOf("같은 그룹 자식"), groups[0].children)
        assertEquals(null, groups[0].aiFeedback)
        assertEquals("무태그 1", groups[1].title)
        assertEquals(null, groups[1].aiFeedback)
        assertEquals("무태그 2", groups[2].title)
        assertEquals(null, groups[2].aiFeedback)
        verify(timetableThinkingAiFeedbackService).appendFeedbackAsync(
            groups,
            listOf("block-1", "block-2", "block-3"),
        )

        assertEquals("page-2", response.pageId)
        assertEquals(1, response.tagCount)
        assertEquals(3, response.appendedTopLevelBulletCount)
        assertEquals(4, response.thinkingCount)
    }

    @Test
    fun `TimetableThinkingAiFeedbackService는 각 그룹 피드백을 생성해 대응 블록에 붙인다`() {
        val aiGenerateService: AiGenerateService = Mockito.mock(AiGenerateService::class.java)
        val slackApiClient: SlackApiClient = Mockito.mock(SlackApiClient::class.java)
        val service =
            TimetableThinkingAiFeedbackService(
                aiGenerateService = aiGenerateService,
                slackApiClient = slackApiClient,
                slackProperties = SlackProperties(botToken = "token", timetableFeedbackChannelId = "channel-1"),
            )
        Mockito.`when`(aiGenerateService.generateText(Mockito.anyString(), Mockito.isNull()))
            .thenReturn("피드백 1", "피드백 2")

        service.appendFeedbackAsync(
            groups =
                listOf(
                    TimetableThinkingGroup(title = "첫 생각", children = listOf("자식")),
                    TimetableThinkingGroup(title = "둘째 생각", children = emptyList()),
                ),
            blockIds = listOf("block-1", "block-2"),
        )

        verify(aiGenerateService, timeout(1000).times(2)).generateText(Mockito.anyString(), Mockito.isNull())
        verify(slackApiClient, timeout(1000).times(2)).postMessage(Mockito.eq("channel-1"), Mockito.anyString())
    }
}
