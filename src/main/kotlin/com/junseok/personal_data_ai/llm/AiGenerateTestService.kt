package com.junseok.personal_data_ai.llm

import org.springframework.stereotype.Service

@Service
class AiGenerateTestService(
    private val aiGenerateService: AiGenerateService,
) {
    fun generate(request: AiGenerateTestRequest): AiGenerateTestResponse {
        val text =
            aiGenerateService.generateText(
                prompt = request.prompt,
                model = request.model,
            )
        return AiGenerateTestResponse(text = text)
    }
}
