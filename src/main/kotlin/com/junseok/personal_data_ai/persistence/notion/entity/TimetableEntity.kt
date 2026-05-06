package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Index
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "timetable", indexes = [
    Index(name = "idx_timetable_record_date", columnList = "record_date", unique = true),
])
class TimetableEntity(
    id: Long? = null,
    notionPageId: String,
    notionLastEditedAt: Instant? = null,
    recordDate: LocalDate? = null,
    title: String,
    @Column(name = "done_work", length = 3000)
    var doneWork: String? = null,
    @Column(name = "tomorrow_tasks", length = 3000)
    var tomorrowTasks: String? = null,
    @Column(name = "retrospective", length = 3000)
    var retrospective: String? = null,
    @Column(name = "feedback", length = 3000)
    var feedback: String? = null,
    @Column(name = "score", precision = 10, scale = 2)
    var score: BigDecimal? = null,
    @Lob
    @Column(name = "content")
    var content: String? = null,
) : DatedNotionPageEntity(
    id = id,
    notionPageId = notionPageId,
    notionLastEditedAt = notionLastEditedAt,
    title = title,
    recordDate = recordDate,
) {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    var condition: ConditionEntity? = null
}
