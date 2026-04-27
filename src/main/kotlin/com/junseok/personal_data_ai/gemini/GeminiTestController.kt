package com.junseok.personal_data_ai.gemini

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/gemini")
class GeminiTestController(
    private val geminiTestService: GeminiTestService,
) {
    @PostMapping("/test")
    fun test(
        @Valid @RequestBody request: GeminiTestRequest,
    ): ResponseEntity<GeminiTestResponse> = ResponseEntity.ok(geminiTestService.generate(request))
}

