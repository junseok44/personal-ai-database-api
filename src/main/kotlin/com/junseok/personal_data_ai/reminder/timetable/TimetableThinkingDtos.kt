package com.junseok.personal_data_ai.reminder.timetable

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class TimetableThinkingAppendRequest(
    @field:Valid
    @field:NotEmpty
    val thinking: List<TimetableThinkingItemRequest>,
)

data class TimetableThinkingItemRequest(
    @field:NotBlank
    val title: String,
    // @field:NotBlank
    val tag: String? = null,
)

data class TimetableThinkingAppendResponse(
    val pageId: String,
    val tagCount: Int,
    val appendedTopLevelBulletCount: Int,
    val thinkingCount: Int,
)

