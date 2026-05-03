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
    private val notionBlockContentFactory: NotionBlockContentFactory,
) {
    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    fun upsertTodayAndUpdateProperties(categoryContent: Map<String, NotionPagePropertyContent>): String {
        val pageId = findOrCreateTodayPage()

        val properties =
            categoryContent.mapValues { (_, content) ->
                mapOf(
                    "rich_text" to notionBlockContentFactory.propertyRichTextArray(content),
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
        val appendedBlockIds =
            if (blocks.isNotEmpty()) {
                notionApiClient.appendBlockChildren(pageId, blocks).map { it.id }.filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        return TimetableThinkingAppendResult(
            pageId = pageId,
            appendedTopLevelBulletCount = groupedThinking.size,
            appendedTopLevelBlockIds = appendedBlockIds,
        )
    }

    fun appendAiFeedbackToThinkingBlock(
        blockId: String,
        feedback: String,
    ) {
        val trimmed = feedback.trim()
        if (blockId.isBlank() || trimmed.isBlank()) return

        notionApiClient.appendBlockChildren(
            blockId,
            listOf(
                notionBlockContentFactory.createToggleBlock(
                    "AI의 피드백",
                    children = listOf(notionBlockContentFactory.createParagraphBlock(trimmed)),
                ),
            ),
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

    private fun buildThinkingBlocks(groupedThinking: List<TimetableThinkingGroup>): List<Map<String, Any>> {
        val blocks = mutableListOf<Map<String, Any>>()
        for (group in groupedThinking) {
            val first = group.title.trim()
            if (first.isBlank()) continue

            val children = mutableListOf<Map<String, Any>>()
            children.addAll(
                group.children
                    .mapNotNull { it.trim().takeIf { t -> t.isNotBlank() } }
                    .map { notionBlockContentFactory.createBulletedBlock(it) },
            )

            val feedback = group.aiFeedback?.trim().orEmpty()
            if (feedback.isNotBlank()) {
                children.add(
                    notionBlockContentFactory.createToggleBlock(
                        "AI의 피드백",
                        children = listOf(notionBlockContentFactory.createParagraphBlock(feedback)),
                    ),
                )
            }

            blocks.add(notionBlockContentFactory.createBulletedBlock(first, children))
        }
        return blocks
    }
}

data class TimetableThinkingAppendResult(
    val pageId: String,
    val appendedTopLevelBulletCount: Int,
    val appendedTopLevelBlockIds: List<String> = emptyList(),
)


