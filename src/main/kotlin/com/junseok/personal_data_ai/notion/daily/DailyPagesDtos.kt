package com.junseok.personal_data_ai.notion.daily

data class DailyPagesUpsertResponse(
    val title: String,
    /** Notion dateProperty에 들어간 start 값(있다면). */
    val dateStart: String?,
    val timetablePageId: String,
    val conditionPageId: String,
    val nutritionPageId: String,
)

data class DailyPagesDeleteResponse(
    val title: String,
    val deleted: DeletedDailyPages,
)

data class DeletedDailyPages(
    val timetablePageId: String?,
    val conditionPageId: String?,
    val nutritionPageId: String?,
)

