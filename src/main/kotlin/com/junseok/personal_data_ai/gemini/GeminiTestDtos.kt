package com.junseok.personal_data_ai.gemini

import jakarta.validation.constraints.NotBlank

data class GeminiTestRequest(
    @field:NotBlank
    val prompt: String,
    val model: String? = null,
)

data class GeminiTestResponse(
    val text: String,
)

