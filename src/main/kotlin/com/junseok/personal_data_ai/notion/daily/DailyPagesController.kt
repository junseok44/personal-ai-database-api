package com.junseok.personal_data_ai.notion.daily

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/daily-pages")
class DailyPagesController(
    private val dailyPagesService: DailyPagesService,
) {
    @PostMapping
    fun upsertAndConnect(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): ResponseEntity<DailyPagesUpsertResponse> =
        ResponseEntity.ok(dailyPagesService.upsertAndConnectDailyPages(date))

    @DeleteMapping
    fun delete(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): ResponseEntity<DailyPagesDeleteResponse> =
        ResponseEntity.ok(dailyPagesService.deleteDailyPages(date))
}

