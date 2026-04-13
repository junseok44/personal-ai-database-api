package com.junseok.personal_data_ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notion")
data class NotionProperties(
    val apiKey: String,
    val databaseId: String,
    val version: String = "2022-06-28",
    val titleProperty: String = "Name",
    /** 일일 페이지 제목(MMdd) 기준 날짜를 잡을 때 사용 (예: Asia/Seoul). JVM 기본 타임존과 무관하게 고정 가능. */
    val dayTimeZone: String = "Asia/Seoul",
)
