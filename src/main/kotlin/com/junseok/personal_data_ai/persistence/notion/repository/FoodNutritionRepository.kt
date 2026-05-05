package com.junseok.personal_data_ai.persistence.notion.repository

import com.junseok.personal_data_ai.persistence.notion.entity.FoodNutritionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.MealType
import org.springframework.data.jpa.repository.JpaRepository

interface FoodNutritionRepository : JpaRepository<FoodNutritionEntity, Long> {
    fun existsByNutrition_IdAndFood_IdAndMealType(
        nutritionId: Long,
        foodId: Long,
        mealType: MealType,
    ): Boolean
}
