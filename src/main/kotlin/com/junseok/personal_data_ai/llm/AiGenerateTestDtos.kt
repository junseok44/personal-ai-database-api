package com.junseok.personal_data_ai.llm

import jakarta.validation.constraints.NotBlank

data class AiGenerateTestRequest(
    @field:NotBlank
    val prompt: String,
    val model: String? = null,
)

data class AiGenerateTestResponse(
    val text: String,
)
