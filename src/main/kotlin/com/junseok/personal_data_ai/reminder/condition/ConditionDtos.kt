package com.junseok.personal_data_ai.reminder.condition

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class ConditionSyncRequest(
    @field:Valid
    @field:NotEmpty
    val conditions: List<ConditionReminderItemRequest>,
)

data class ConditionReminderItemRequest(
    @field:NotBlank
    val title: String,
)

data class ConditionSyncResponse(
    val pageId: String,
    val mergedCount: Int,
    val reminderCount: Int,
)
