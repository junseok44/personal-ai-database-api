package com.junseok.personal_data_ai.persistence.notion.repository

import com.junseok.personal_data_ai.persistence.notion.entity.TimetableEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface TimetableRepository : JpaRepository<TimetableEntity, Long> {
    fun findByNotionPageId(notionPageId: String): Optional<TimetableEntity>

    fun findByRecordDate(recordDate: LocalDate): Optional<TimetableEntity>

    fun findAllByRecordDateIn(recordDates: Collection<LocalDate>): List<TimetableEntity>

    fun findAllByRecordDateIsBetween(startDate: LocalDate, endDate: LocalDate): List<TimetableEntity>

    fun deleteAllByRecordDateIn(recordDates: Collection<LocalDate>)
}
