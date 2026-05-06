package com.junseok.personal_data_ai.notion.sync

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/notion-data")
class NotionDataController(
    private val notionDataService: NotionDataService,
) {
    @GetMapping
    fun getNotionData(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate,
    ): ResponseEntity<NotionDataResponse> =
        ResponseEntity.ok(notionDataService.getNotionData(startDate, endDate))

    @GetMapping("/refresh")
    fun refreshNotionData(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate,
    ): ResponseEntity<NotionDataResponse> =
        ResponseEntity.ok(notionDataService.refreshNotionData(startDate, endDate))
}
