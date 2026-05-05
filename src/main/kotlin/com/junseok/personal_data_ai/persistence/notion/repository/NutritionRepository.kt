package com.junseok.personal_data_ai.persistence.notion.repository

import com.junseok.personal_data_ai.persistence.notion.entity.NutritionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface NutritionRepository : JpaRepository<NutritionEntity, Long> {
    fun findByNotionPageId(notionPageId: String): Optional<NutritionEntity>

    fun findByRecordDate(recordDate: LocalDate): Optional<NutritionEntity>

    fun findByTitle(title: String): Optional<NutritionEntity>

    fun findAllByRecordDateIn(recordDates: Collection<LocalDate>): List<NutritionEntity>

    fun findAllByRecordDateIsBetween(startDate: LocalDate, endDate: LocalDate): List<NutritionEntity>

    fun deleteAllByRecordDateIn(recordDates: Collection<LocalDate>)
}