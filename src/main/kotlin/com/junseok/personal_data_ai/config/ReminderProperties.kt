package com.junseok.personal_data_ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "reminder")
data class ReminderProperties(
    val allowedCategories: List<String> = emptyList(),
)
