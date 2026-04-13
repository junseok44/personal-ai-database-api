package com.junseok.personal_data_ai.reminder

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class ReminderSyncRequest(
    @field:Valid
    @field:NotEmpty
    val reminders: List<ReminderItemRequest>,
)

data class ReminderItemRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val category: String,
)

data class ReminderSyncResponse(
    val pageId: String,
    val categoryCount: Int,
    val reminderCount: Int,
)
