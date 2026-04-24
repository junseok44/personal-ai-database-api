package com.junseok.personal_data_ai.notion.daily

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionApiClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class DailyPagesService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
    private val dailyPageUpsertHelper: DailyPageUpsertHelper,
) {
    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    private val timetableConditionRelationPropertyName = "헬스체크"
    private val conditionNutritionRelationPropertyName = "식사/영양"

    fun upsertAndConnectDailyPages(targetDate: LocalDate?): DailyPagesUpsertResponse {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)

        val calendarDate = targetDate ?: LocalDate.now(zoneId)
        val title = calendarDate.format(titleFormatter)

        val requestedAt = ZonedDateTime.now(zoneId)
        val dateTimeForProperty = requestedAt.with(calendarDate)
        val dateStart = buildDateStartForNotion(dateTimeForProperty)

        val conditionPageId =
            upsertConditionPage(
                databaseId = notionProperties.conditionDatabaseId,
                title = title,
                dateStart = dateStart,
            )

        val nutritionPageId =
            upsertNutritionPage(
                databaseId = notionProperties.nutritionDatabaseId,
                title = title,
                dateStart = dateStart,
            )

        val timetablePageId =
            upsertTimetablePage(
                databaseId = notionProperties.timetableDatabaseId,
                title = title,
                dateStart = dateStart,
            )

        // condition -> nutrition 연결 (dual relation이라 nutrition 쪽도 자동 동기화됨)
        notionApiClient.patchPageProperties(
            pageId = conditionPageId,
            properties =
                mapOf(
                    conditionNutritionRelationPropertyName to relationValue(nutritionPageId),
                ),
        )

        // timetable -> condition 연결
        notionApiClient.patchPageProperties(
            pageId = timetablePageId,
            properties =
                mapOf(
                    timetableConditionRelationPropertyName to relationValue(conditionPageId),
                ),
        )

        return DailyPagesUpsertResponse(
            title = title,
            dateStart = dateStart,
            timetablePageId = timetablePageId,
            conditionPageId = conditionPageId,
            nutritionPageId = nutritionPageId,
        )
    }

    fun deleteDailyPages(targetDate: LocalDate?): DailyPagesDeleteResponse {
        val zoneId = ZoneId.of(notionProperties.dayTimeZone)
        val calendarDate = targetDate ?: LocalDate.now(zoneId)
        val title = calendarDate.format(titleFormatter)

        val timetablePageId =
            notionApiClient.queryDatabaseByTitleEquals(
                databaseId = notionProperties.timetableDatabaseId,
                titleProperty = notionProperties.titleProperty,
                titleEquals = title,
            )
        val conditionPageId =
            notionApiClient.queryDatabaseByTitleEquals(
                databaseId = notionProperties.conditionDatabaseId,
                titleProperty = notionProperties.titleProperty,
                titleEquals = title,
            )
        val nutritionPageId =
            notionApiClient.queryDatabaseByTitleEquals(
                databaseId = notionProperties.nutritionDatabaseId,
                titleProperty = notionProperties.titleProperty,
                titleEquals = title,
            )

        timetablePageId?.let(notionApiClient::archivePage)
        conditionPageId?.let(notionApiClient::archivePage)
        nutritionPageId?.let(notionApiClient::archivePage)

        return DailyPagesDeleteResponse(
            title = title,
            deleted =
                DeletedDailyPages(
                    timetablePageId = timetablePageId,
                    conditionPageId = conditionPageId,
                    nutritionPageId = nutritionPageId,
                ),
        )
    }

    private fun upsertTimetablePage(
        databaseId: String,
        title: String,
        dateStart: String?,
    ): String {
        return dailyPageUpsertHelper.findOrCreatePage(databaseId, title, dateStart)
    }

    private fun upsertConditionPage(
        databaseId: String,
        title: String,
        dateStart: String?,
    ): String {
        return dailyPageUpsertHelper.findOrCreatePage(databaseId, title, dateStart)
    }

    private fun upsertNutritionPage(
        databaseId: String,
        title: String,
        dateStart: String?,
    ): String {
        return dailyPageUpsertHelper.findOrCreatePage(databaseId, title, dateStart)
    }

    private fun relationValue(pageId: String): Map<String, Any> =
        mapOf(
            "relation" to listOf(mapOf("id" to pageId)),
        )

    private fun buildDateStartForNotion(dateTime: ZonedDateTime): String? {
        if (notionProperties.dateProperty.isBlank()) return null
        return OffsetDateTime.from(dateTime).toString()
    }
}

