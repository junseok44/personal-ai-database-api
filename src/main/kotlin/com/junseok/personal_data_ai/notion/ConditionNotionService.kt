package com.junseok.personal_data_ai.notion

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.daily.DailyPageUpsertHelper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ConditionNotionService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
    private val dailyPageUpsertHelper: DailyPageUpsertHelper,
    private val notionBlockContentFactory: NotionBlockContentFactory,
) {
    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    fun upsertTodayAndMerge(entries: List<ConditionMergeEntry>): ConditionMergeResult {
        val pageId = findOrCreateTodayPage()
        val mergedCount = mergeBullets(pageId, entries)
        return ConditionMergeResult(
            pageId = pageId,
            mergedCount = mergedCount,
        )
    }

    private fun findOrCreateTodayPage(): String {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val calendarDate = LocalDate.now(zoneId)
        val todayTitle = calendarDate.format(titleFormatter)
        val dateStart = if (notionProperties.dateProperty.isNotBlank()) calendarDate.toString() else null
        return dailyPageUpsertHelper.findOrCreatePage(
            databaseId = notionProperties.conditionDatabaseId,
            title = todayTitle,
            dateStart = dateStart,
        )
    }

    private fun mergeBullets(
        pageId: String,
        entries: List<ConditionMergeEntry>,
    ): Int {
        if (entries.isEmpty()) return 0

        // 1) 기존 top-level header 블록 id 수집 (key: HH:mm)
        val existingTopBlocks = notionApiClient.fetchAllBlockChildren(pageId)
        val existingHeaderBlockByTime = linkedMapOf<String, ExistingHeaderBlock>() // time -> (blockId, headerText)
        for (block in existingTopBlocks) {
            if (block.type != "bulleted_list_item") continue
            val headerText = block.bulleted_list_item.toPlainText().trim()
            val time = extractTime(headerText) ?: continue
            existingHeaderBlockByTime.putIfAbsent(time, ExistingHeaderBlock(block.id, headerText))
        }

        // 2) 요청을 time 기준으로 그룹핑하고, tag는 있으면 하나만 선택
        val requestedTextsByTime = linkedMapOf<String, LinkedHashSet<String>>()
        val requestedTagByTime = linkedMapOf<String, String?>()
        for (entry in entries) {
            val time = entry.time.trim()
            val text = entry.text.trim()
            if (time.isBlank() || text.isBlank()) continue
            requestedTextsByTime.getOrPut(time) { linkedSetOf() }.add(text)
            if (!requestedTagByTime.containsKey(time)) {
                requestedTagByTime[time] = entry.tag?.trim()?.takeIf { it.isNotBlank() }
            } else if (requestedTagByTime[time].isNullOrBlank()) {
                requestedTagByTime[time] = entry.tag?.trim()?.takeIf { it.isNotBlank() }
            }
        }
        if (requestedTextsByTime.isEmpty()) return 0

        var mergedCount = 0

        // 3) 기존 time 헤더가 있으면: 필요시 헤더 텍스트를 "HH:mm / tag"로 업데이트 + 자식만 append
        for ((time, requestedTexts) in requestedTextsByTime) {
            val existing = existingHeaderBlockByTime[time] ?: continue
            val desiredTag = requestedTagByTime[time]
            val desiredHeaderText = buildHeaderText(time, desiredTag)
            if (desiredTag != null && !existing.headerText.contains(" / ")) {
                notionApiClient.updateBulletedListItemText(existing.blockId, desiredHeaderText)
                existingHeaderBlockByTime[time] = existing.copy(headerText = desiredHeaderText)
            }

            val children = notionApiClient.fetchAllBlockChildren(existing.blockId)
            val existingChildTexts = linkedSetOf<String>()
            for (child in children) {
                if (child.type != "bulleted_list_item") continue
                val childText = child.bulleted_list_item.toPlainText().trim()
                if (childText.isNotBlank()) existingChildTexts.add(childText)
            }

            val toAppend = requestedTexts.filter { it !in existingChildTexts }
            if (toAppend.isNotEmpty()) {
                mergedCount += toAppend.size
                notionApiClient.appendBlockChildren(existing.blockId, toAppend.map { notionBlockContentFactory.createBulletedBlock(it) })
            }
        }

        // 4) 새 time 헤더는 top-level block(자식 포함)을 한 번에 append
        val newHeaderBlocks = mutableListOf<Map<String, Any>>()
        for ((time, requestedTexts) in requestedTextsByTime) {
            if (existingHeaderBlockByTime.containsKey(time)) continue
            if (requestedTexts.isEmpty()) continue
            mergedCount += requestedTexts.size
            val headerText = buildHeaderText(time, requestedTagByTime[time])
            newHeaderBlocks.add(
                notionBlockContentFactory.createBulletedBlock(
                    headerText,
                    children = requestedTexts.map { notionBlockContentFactory.createBulletedBlock(it) },
                ),
            )
        }
        if (newHeaderBlocks.isNotEmpty()) {
            notionApiClient.appendBlockChildren(pageId, newHeaderBlocks)
        }

        return mergedCount
    }

    private fun NotionBulletedListItem?.toPlainText(): String =
        this?.rich_text?.joinToString("") { it.plain_text } ?: ""

    private fun extractTime(headerText: String): String? {
        val trimmed = headerText.trim()
        if (trimmed.length < 5) return null
        val candidate = trimmed.substring(0, 5)
        return if (candidate[2] == ':' && candidate[0].isDigit() && candidate[1].isDigit() && candidate[3].isDigit() && candidate[4].isDigit()) {
            candidate
        } else {
            null
        }
    }

    private fun buildHeaderText(
        time: String,
        tag: String?,
    ): String = if (tag.isNullOrBlank()) time else "$time / $tag"

    private data class ExistingHeaderBlock(
        val blockId: String,
        val headerText: String,
    )
}

data class ConditionMergeEntry(
    val time: String,
    val tag: String?,
    val text: String,
)

data class ConditionMergeResult(
    val pageId: String,
    val mergedCount: Int,
)

