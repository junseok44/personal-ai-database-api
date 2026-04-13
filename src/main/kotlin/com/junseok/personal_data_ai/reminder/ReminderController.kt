package com.junseok.personal_data_ai.reminder

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reminders")
class ReminderController(
    private val reminderSyncService: ReminderSyncService,
) {
    @PostMapping
    fun syncReminders(
        @Valid @RequestBody request: ReminderSyncRequest,
    ): ResponseEntity<ReminderSyncResponse> = ResponseEntity.ok(reminderSyncService.sync(request))
}
