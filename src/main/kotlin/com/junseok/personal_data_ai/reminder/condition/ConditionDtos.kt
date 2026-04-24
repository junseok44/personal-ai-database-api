package com.junseok.personal_data_ai.reminder.condition

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.OffsetDateTime

data class ConditionSyncRequest(
    @field:Valid
    @field:NotEmpty
    val conditions: List<ConditionReminderItemRequest>,
)

data class ConditionReminderItemRequest(
    @field:NotBlank
    val title: String,
    /**
     * 미리알림 생성 시각. ISO 8601 datetime (예: 2026-04-24T08:36:00+09:00)
     */
    val time: OffsetDateTime,
    /**
     * 선택 태그. 있으면 "time/tag" 형식으로 기록.
     */
    val tag: String? = null,
)
 
data class ConditionSyncResponse(
    val pageId: String,
    val mergedCount: Int,
    val reminderCount: Int,
)
