package com.junseok.personal_data_ai.reminder.timetable

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/timetable")
class TimetableController(
    private val timetableSyncService: TimetableSyncService,
) {
    @PostMapping("/reminders")
    fun syncTimetableReminders(
        @Valid @RequestBody request: TimetableSyncRequest,
    ): ResponseEntity<TimetableSyncResponse> = ResponseEntity.ok(timetableSyncService.sync(request))

    @PostMapping("/thinking")
    fun appendThinking(
        @Valid @RequestBody request: TimetableThinkingAppendRequest,
    ): ResponseEntity<TimetableThinkingAppendResponse> =
        ResponseEntity.ok(timetableSyncService.appendThinking(request))
}

