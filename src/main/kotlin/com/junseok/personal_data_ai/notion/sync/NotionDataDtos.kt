package com.junseok.personal_data_ai.notion.sync

import com.junseok.personal_data_ai.persistence.notion.entity.ConditionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.MealType
import com.junseok.personal_data_ai.persistence.notion.entity.NutritionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.TimetableEntity
import java.math.BigDecimal
import java.time.LocalDate

data class NotionDataResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val fetched: NotionDataFetchedCount,
    val days: List<DailyNotionDataResponse>,
)

data class NotionDataFetchedCount(
    val timetables: Int,
    val conditions: Int,
    val nutritions: Int,
    val foods: Int,
)

data class DailyNotionDataResponse(
    val date: LocalDate,
    val timetable: TimetableResponse?,
    val condition: ConditionResponse?,
    val nutrition: NutritionResponse?,
)

data class TimetableResponse(
    val notionPageId: String,
    val title: String,
    val doneWork: String?,
    val tomorrowTasks: String?,
    val retrospective: String?,
    val feedback: String?,
    val content: String?,
)

data class ConditionResponse(
    val notionPageId: String,
    val title: String,
    val conditionScore: String?,
    val statusText: String?,
    val expectedCause: String?,
    val improvedReason: String?,
    val rawContent: String?,
)

data class NutritionResponse(
    val notionPageId: String,
    val title: String,
    val foodsByMealType: Map<MealType, List<FoodResponse>>,
)

data class FoodResponse(
    val notionPageId: String,
    val title: String,
    val calories: BigDecimal?,
    val carbohydrates: BigDecimal?,
    val protein: BigDecimal?,
    val fat: BigDecimal?,
    val sugar: BigDecimal?,
    val sodium: BigDecimal?,
    val saturatedFat: BigDecimal?,
)

fun TimetableEntity.toResponse(): TimetableResponse =
    TimetableResponse(
        notionPageId = notionPageId,
        title = title,
        doneWork = doneWork,
        tomorrowTasks = tomorrowTasks,
        retrospective = retrospective,
        feedback = feedback,
        content = content,
    )

fun ConditionEntity.toResponse(): ConditionResponse =
    ConditionResponse(
        notionPageId = notionPageId,
        title = title,
        conditionScore = conditionScore?.stripTrailingZeros()?.toPlainString(),
        statusText = statusText,
        expectedCause = expectedCause,
        improvedReason = improvedReason,
        rawContent = rawContent,
    )

fun NutritionEntity.toResponse(): NutritionResponse =
    NutritionResponse(
        notionPageId = notionPageId,
        title = title,
        foodsByMealType =
            foodNutritions
                .groupBy { it.mealType }
                .mapValues { (_, links) ->
                    links.map {
                        FoodResponse(
                            notionPageId = it.food.notionPageId,
                            title = it.food.title,
                            calories = it.food.calories,
                            carbohydrates = it.food.carbohydrates,
                            protein = it.food.protein,
                            fat = it.food.fat,
                            sugar = it.food.sugar,
                            sodium = it.food.sodium,
                            saturatedFat = it.food.saturatedFat,
                        )
                    }
                },
    )
