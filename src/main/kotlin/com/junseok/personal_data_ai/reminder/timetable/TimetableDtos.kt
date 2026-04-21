package com.junseok.personal_data_ai.reminder.timetable

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class TimetableSyncRequest(
    @field:Valid
    @field:NotEmpty
    val reminders: List<TimetableItemRequest>,
)

data class TimetableItemRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val category: String,
    /** DoneTodos(카테고리명 `한 일`)에서만 사용. 10~50(5단위) 또는 문자열. 비어 있으면 자잘한 일로 처리. */
    val tag: Any? = null,
)

data class TimetableSyncResponse(
    val pageId: String,
    val categoryCount: Int,
    val reminderCount: Int,
)

