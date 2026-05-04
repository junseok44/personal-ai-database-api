package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "food_nutrition",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_food_nutrition_meal",
            columnNames = ["food_id", "nutrition_id", "meal_type"],
        ),
    ],
)
class FoodNutritionEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    var food: FoodEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrition_id", nullable = false)
    var nutrition: NutritionEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    var mealType: MealType,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    SNACK,
    DINNER,
}