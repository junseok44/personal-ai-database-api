package com.junseok.personal_data_ai.reminder.condition

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/conditions")
class ConditionController(
    private val conditionSyncService: ConditionSyncService,
) {
    @PostMapping("/reminders")
    fun syncConditionReminders(
        @Valid @RequestBody request: ConditionSyncRequest,
    ): ResponseEntity<ConditionSyncResponse> = ResponseEntity.ok(conditionSyncService.sync(request))
}
