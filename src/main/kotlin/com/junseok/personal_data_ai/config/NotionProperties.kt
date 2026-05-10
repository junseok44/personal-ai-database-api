package com.junseok.personal_data_ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notion")
data class NotionProperties(
    val apiKey: String,
    /**
     * 레거시 기본 DB. (가능하면 사용하지 말고 아래의 목적별 DB id를 사용)
     * 기존 설정 호환을 위해 남겨둔다.
     */
    /** 레거시 기본 DB. (호환을 위해 남김) */
    val timetableDatabaseId: String = "",
    val tilDatabaseId: String,
    val conditionDatabaseId: String,
    val nutritionDatabaseId: String,
    val foodDatabaseId: String,
    val weatherPageId: String = "",
    val version: String = "2022-06-28",
    val titleProperty: String = "Name",
    /** 일일 페이지 제목(MMdd) 기준 날짜를 잡을 때 사용 (예: Asia/Seoul). JVM 기본 타임존과 무관하게 고정 가능. */
    val dayTimeZone: String = "Asia/Seoul",
    /** 신규 페이지 생성 시 채울 Notion Date 타입 컬럼 이름. 비어 있으면 날짜 프로퍼티를 보내지 않음. */
    val dateProperty: String = "",
)
