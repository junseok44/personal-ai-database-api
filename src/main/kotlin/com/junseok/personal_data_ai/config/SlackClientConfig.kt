package com.junseok.personal_data_ai.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
class SlackClientConfig {
    @Bean
    fun slackRestClient(
        builder: RestClient.Builder,
        slackProperties: SlackProperties,
    ): RestClient =
        builder
            .baseUrl("https://slack.com/api")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${slackProperties.botToken}")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
