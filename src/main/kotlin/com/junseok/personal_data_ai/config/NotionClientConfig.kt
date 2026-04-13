package com.junseok.personal_data_ai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
class NotionClientConfig {
    @Bean
    fun notionRestClient(
        builder: RestClient.Builder,
        notionProperties: NotionProperties,
    ): RestClient =
        builder
            .baseUrl("https://api.notion.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${notionProperties.apiKey}")
            .defaultHeader("Notion-Version", notionProperties.version)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
