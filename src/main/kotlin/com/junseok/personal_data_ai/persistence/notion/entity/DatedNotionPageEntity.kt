package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import java.time.LocalDate

@MappedSuperclass
abstract class DatedNotionPageEntity(
    id: Long? = null,
    notionPageId: String,
    notionLastEditedAt: Instant? = null,
    title: String,
    @Column(name = "record_date")
    var recordDate: LocalDate? = null,
): NotionPageEntity(
    id = id,
    notionPageId = notionPageId,
    notionLastEditedAt = notionLastEditedAt,
    title = title,
)