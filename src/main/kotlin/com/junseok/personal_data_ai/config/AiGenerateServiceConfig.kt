package com.junseok.personal_data_ai.config

import com.junseok.personal_data_ai.llm.AiGenerateService
import com.junseok.personal_data_ai.llm.gemini.GeminiAiGenerateService
import com.junseok.personal_data_ai.llm.gemini.GeminiApiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiGenerateServiceConfig {
    @Bean
    fun aiGenerateService(geminiApiClient: GeminiApiClient): AiGenerateService = GeminiAiGenerateService(geminiApiClient)
}

