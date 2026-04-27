package com.junseok.personal_data_ai.config

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class GeminiClientConfig {
    @Bean
    fun geminiRestClient(
        builder: RestClient.Builder,
        geminiProperties: GeminiProperties,
    ): RestClient =
        builder
            .baseUrl("https://generativelanguage.googleapis.com/${geminiProperties.apiVersion}")
            .defaultHeader("x-goog-api-key", geminiProperties.apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
