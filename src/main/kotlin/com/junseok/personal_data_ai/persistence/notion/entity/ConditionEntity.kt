package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Lob
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "condition")
class ConditionEntity(
    id: Long? = null,
    notionPageId: String,
    notionLastEditedAt: Instant? = null,
    recordDate: LocalDate? = null,
    title: String,
    @Column(name = "symptom_text")
    var symptomText: String? = null,
    @Column(name = "prescription_taken_count")
    var prescriptionTakenCount: Int? = null,
    @Column(name = "digestive_taken_count")
    var digestiveTakenCount: Int? = null,
    @Column(name = "vitamin_b_taken_count")
    var vitaminBTakenCount: Int? = null,
    @Column(name = "weight_kg", precision = 10, scale = 2)
    var weightKg: BigDecimal? = null,
    @Column(name = "sleep_hours", length = 100)
    var sleepHours: String? = null,
    @Column(name = "condition_score", precision = 10, scale = 2)
    var conditionScore: BigDecimal? = null,
    @Column(name = "status_text")
    var statusText: String? = null,
    @Column(name = "expected_cause", length = 3000)
    var expectedCause: String? = null,
    @Column(name = "improved_reason", length = 3000)
    var improvedReason: String? = null,
    @Lob
    @Column(name = "raw_content")
    var rawContent: String? = null,
) : DatedNotionPageEntity(
    id = id,
    notionPageId = notionPageId,
    notionLastEditedAt = notionLastEditedAt,
    title = title,
    recordDate = recordDate,
) {
    @OneToOne(mappedBy = "condition", fetch = FetchType.LAZY)
    var timetable: TimetableEntity? = null
}