package com.junseok.personal_data_ai.persistence.notion.repository

import com.junseok.personal_data_ai.persistence.notion.entity.FoodEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface FoodRepository : JpaRepository<FoodEntity, Long> {
    fun findByNotionPageId(notionPageId: String): Optional<FoodEntity>

    fun findByTitle(title: String): Optional<FoodEntity>
}