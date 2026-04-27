package com.junseok.personal_data_ai.gemini

import org.springframework.stereotype.Service

@Service
class GeminiTestService(
    private val geminiApiClient: GeminiApiClient,
) {
    fun generate(request: GeminiTestRequest): GeminiTestResponse {
        val model = request.model
        val text =
            if (model.isNullOrBlank()) {
                geminiApiClient.generateText(prompt = request.prompt)
            } else {
                geminiApiClient.generateText(prompt = request.prompt, model = model)
            }
        return GeminiTestResponse(text = text)
    }
}

