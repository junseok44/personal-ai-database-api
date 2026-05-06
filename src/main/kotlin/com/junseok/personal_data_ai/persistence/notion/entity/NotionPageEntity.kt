package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.Instant

@MappedSuperclass
abstract class NotionPageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "notion_page_id", nullable = false, unique = true, length = 64)
    var notionPageId: String,
    @Column(name = "notion_last_edited_at")
    var notionLastEditedAt: Instant? = null,
    @Column(name = "title", nullable = false, length = 200)
    var title: String,
)
