package com.junseok.personal_data_ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "slack")
data class SlackProperties(
    val botToken: String,
    val timetableFeedbackChannelId: String,
)
