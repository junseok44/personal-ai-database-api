package com.junseok.personal_data_ai.llm

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/llm")
class AiGenerateTestController(
    private val aiGenerateTestService: AiGenerateTestService,
) {
    @PostMapping("/generate/test")
    fun test(
        @Valid @RequestBody request: AiGenerateTestRequest,
    ): ResponseEntity<AiGenerateTestResponse> =
        ResponseEntity.ok(aiGenerateTestService.generate(request))
}
