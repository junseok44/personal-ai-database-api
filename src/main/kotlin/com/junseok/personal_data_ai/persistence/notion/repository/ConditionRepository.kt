package com.junseok.personal_data_ai.persistence.notion.repository

import com.junseok.personal_data_ai.persistence.notion.entity.ConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import java.time.LocalDate
import java.util.Optional

interface ConditionRepository : JpaRepository<ConditionEntity, Long> {
    fun findByNotionPageId(notionPageId: String): Optional<ConditionEntity>

    fun findByRecordDate(recordDate: LocalDate): Optional<ConditionEntity>

    fun findByTitle(title: String): Optional<ConditionEntity>

    fun findAllByRecordDateIn(recordDates: Collection<LocalDate>): List<ConditionEntity>

    fun findAllByRecordDateIsBetween(startDate: LocalDate, endDate: LocalDate): List<ConditionEntity>

    fun deleteAllByRecordDateIn(recordDates: Collection<LocalDate>)
}