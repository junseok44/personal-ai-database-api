package com.junseok.personal_data_ai.notion.sync

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionPageWithContent
import com.junseok.personal_data_ai.persistence.notion.entity.ConditionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.FoodEntity
import com.junseok.personal_data_ai.persistence.notion.entity.MealType
import com.junseok.personal_data_ai.persistence.notion.entity.NutritionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.TimetableEntity
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class TimetableNotionMapper(
    private val notionProperties: NotionProperties,
) {
    fun toEntity(
        source: NotionPageWithContent,
        fallbackDate: LocalDate,
    ): TimetableEntity {
        val properties = NotionPropertyReader(source.page.properties)
        return TimetableEntity(
            notionPageId = source.page.id,
            notionLastEditedAt = source.page.last_edited_time.toInstantOrNull(),
            recordDate = properties.date(notionProperties.dateProperty) ?: fallbackDate,
            title = properties.title(notionProperties.titleProperty) ?: fallbackDate.toTitle(),
            doneWork = properties.richText("한 일"),
            tomorrowTasks = properties.richText("내일 할 일") ?: properties.richText("내일 할일"),
            retrospective = properties.richText("회고"),
            feedback = properties.richText("피드백"),
            score = properties.number("점수"),
            content = source.blocks.toPlainContent(),
        )
    }
}

@Component
class ConditionNotionMapper(
    private val notionProperties: NotionProperties,
) {
    fun toEntity(
        source: NotionPageWithContent,
        fallbackDate: LocalDate,
    ): ConditionEntity {
        val properties = NotionPropertyReader(source.page.properties)
        return ConditionEntity(
            notionPageId = source.page.id,
            notionLastEditedAt = source.page.last_edited_time.toInstantOrNull(),
            recordDate = properties.date(notionProperties.dateProperty) ?: fallbackDate,
            title = properties.title(notionProperties.titleProperty) ?: fallbackDate.toTitle(),
            symptomText = properties.richText("증상"),
            prescriptionTakenCount = properties.number("처방전 복용")?.toInt(),
            digestiveTakenCount = properties.number("소화제 복용")?.toInt(),
            vitaminBTakenCount = properties.number("비타민B 복용")?.toInt(),
            weightKg = properties.number("몸무게"),
            sleepHours = properties.richText("수면시간"),
            conditionScore = properties.number("컨디션"),
            statusText = properties.richText("상태"),
            expectedCause = properties.richText("예상 원인"),
            improvedReason = properties.richText("좋아진 이유"),
            rawContent = source.blocks.toPlainContent(),
        )
    }
}

@Component
class NutritionNotionMapper(
    private val notionProperties: NotionProperties,
) {
    fun toMapping(
        source: NotionPageWithContent,
        fallbackDate: LocalDate,
    ): NutritionMapping {
        val properties = NotionPropertyReader(source.page.properties)
        val entity =
            NutritionEntity(
                notionPageId = source.page.id,
                notionLastEditedAt = source.page.last_edited_time.toInstantOrNull(),
                recordDate = properties.date(notionProperties.dateProperty) ?: fallbackDate,
                title = properties.title(notionProperties.titleProperty) ?: fallbackDate.toTitle(),
            )
        return NutritionMapping(
            entity = entity,
            foodPageIdsByMealType =
                mapOf(
                    MealType.BREAKFAST to properties.relationIds("아침"),
                    MealType.LUNCH to properties.relationIds("점심"),
                    MealType.SNACK to properties.relationIds("간식"),
                    MealType.DINNER to properties.relationIds("저녁"),
                ),
        )
    }
}

@Component
class FoodNotionMapper(
    private val notionProperties: NotionProperties,
) {
    fun toEntity(source: NotionPageWithContent): FoodEntity {
        val properties = NotionPropertyReader(source.page.properties)
        return FoodEntity(
            notionPageId = source.page.id,
            notionLastEditedAt = source.page.last_edited_time.toInstantOrNull(),
            title = properties.title(notionProperties.titleProperty) ?: source.page.id,
            calories = properties.number("칼로리"),
            carbohydrates = properties.number("탄수화물"),
            protein = properties.number("단백질"),
            fat = properties.number("지방"),
            sugar = properties.number("당"),
            sodium = properties.number("나트륨"),
            saturatedFat = properties.number("포화지방"),
        )
    }
}

data class NutritionMapping(
    val entity: NutritionEntity,
    val foodPageIdsByMealType: Map<MealType, List<String>>,
)

private fun LocalDate.toTitle(): String = "%02d%02d".format(monthValue, dayOfMonth)
