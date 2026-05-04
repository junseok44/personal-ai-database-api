package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

// 이 식단에는 아침, 점심, 간식, 저녁이 있고.
// 그거 각각 별로 food 엔티티와 다대다 관계이다.

@Entity
@Table(name = "nutrition")
class NutritionEntity(
    id: Long? = null,
    notionPageId: String,
    notionLastEditedAt: Instant? = null,
    recordDate: LocalDate? = null,
    title: String,
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

    @OneToMany(mappedBy = "nutrition", cascade = [CascadeType.ALL], orphanRemoval = true)
    val foodNutritions: MutableList<FoodNutritionEntity> = mutableListOf()

    fun addFoodNutrition(food: FoodEntity, mealType: MealType) {
        val foodNutrition = FoodNutritionEntity(
            food = food,
            nutrition = this,
            mealType = mealType,
        )
        foodNutritions.add(foodNutrition)
    }

    fun removeFoodNutrition(food: FoodEntity, mealType: MealType) {
        foodNutritions.removeIf { it.food == food && it.mealType == mealType }
    }

    fun getFoodNutrition(mealType: MealType): FoodNutritionEntity? {
        return foodNutritions.firstOrNull { it.mealType == mealType }
    }
}