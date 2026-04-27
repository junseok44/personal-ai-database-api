package com.junseok.personal_data_ai.llm

interface AiGenerateService {
    fun generateText(
        prompt: String,
        model: String? = null,
    ): String
}

