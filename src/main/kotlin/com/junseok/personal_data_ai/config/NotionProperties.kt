package com.junseok.personal_data_ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notion")
data class NotionProperties(
    val apiKey: String,
    val databaseId: String,
    val version: String = "2022-06-28",
    val titleProperty: String = "Name",
)
