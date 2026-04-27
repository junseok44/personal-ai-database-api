package com.junseok.personal_data_ai.llm.gemini

import com.junseok.personal_data_ai.llm.AiGenerateService

class GeminiAiGenerateService(
    private val geminiApiClient: GeminiApiClient,
) : AiGenerateService {
    override fun generateText(
        prompt: String,
        model: String?,
    ): String {
        return if (model.isNullOrBlank()) {
            geminiApiClient.generateText(prompt = prompt)
        } else {
            geminiApiClient.generateText(prompt = prompt, model = model)
        }
    }
}

