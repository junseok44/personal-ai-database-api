package com.junseok.personal_data_ai.notion.sync

import com.junseok.personal_data_ai.config.NotionProperties
import com.junseok.personal_data_ai.notion.NotionApiClient
import com.junseok.personal_data_ai.persistence.notion.entity.ConditionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.FoodEntity
import com.junseok.personal_data_ai.persistence.notion.entity.NutritionEntity
import com.junseok.personal_data_ai.persistence.notion.entity.TimetableEntity
import com.junseok.personal_data_ai.persistence.notion.repository.ConditionRepository
import com.junseok.personal_data_ai.persistence.notion.repository.FoodRepository
import com.junseok.personal_data_ai.persistence.notion.repository.NutritionRepository
import com.junseok.personal_data_ai.persistence.notion.repository.TimetableRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class NotionDataService(
    private val notionApiClient: NotionApiClient,
    private val notionProperties: NotionProperties,
    private val timetableRepository: TimetableRepository,
    private val conditionRepository: ConditionRepository,
    private val nutritionRepository: NutritionRepository,
    private val foodRepository: FoodRepository,
    private val timetableMapper: TimetableNotionMapper,
    private val conditionMapper: ConditionNotionMapper,
    private val nutritionMapper: NutritionNotionMapper,
    private val foodMapper: FoodNotionMapper,
) {
    @Transactional
    fun refreshNotionData(
        startDate: LocalDate,
        endDate: LocalDate,
    ): NotionDataResponse {
        val dates = startDate.datesUntil(endDate.plusDays(1)).toList()

        nutritionRepository.deleteAllByRecordDateIn(dates)
        timetableRepository.deleteAllByRecordDateIn(dates)
        conditionRepository.deleteAllByRecordDateIn(dates)

        val conditionResult = syncConditions(dates)
        val conditionsByDate = conditionResult.entities.associateByRecordDate()

        val timetableResult = syncTimetables(dates, conditionsByDate)
        val nutritionResult = syncNutritions(dates, conditionsByDate)

        return NotionDataResponse(
            startDate = startDate,
            endDate = endDate,
            fetched =
                NotionDataFetchedCount(
                    timetables = timetableResult.fetchedCount,
                    conditions = conditionResult.fetchedCount,
                    nutritions = nutritionResult.fetchedCount,
                    foods = nutritionResult.fetchedFoodCount,
                ),
            days =
                dates.map { date ->
                    DailyNotionDataResponse(
                        date = date,
                        timetable = timetableResult.entities.find { it.recordDate == date }?.toResponse(),
                        condition = conditionResult.entities.find { it.recordDate == date }?.toResponse(),
                        nutrition = nutritionResult.entities.find { it.recordDate == date }?.toResponse(),
                    )
                },
        )
    }

    private val titleFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMdd")

    @Transactional
    fun getNotionData(
        startDate: LocalDate,
        endDate: LocalDate,
    ): NotionDataResponse {
        require(!endDate.isBefore(startDate)) { "endDate must be greater than or equal to startDate" }
        require(notionProperties.dateProperty.isNotBlank()) { "notion.date-property must be configured for date based sync" }

        val dates = startDate.datesUntil(endDate.plusDays(1)).toList()

        val conditionResult = syncConditions(dates)
        val conditionsByDate = conditionResult.entities.associateByRecordDate()

        val timetableResult = syncTimetables(dates, conditionsByDate)
        val nutritionResult = syncNutritions(dates, conditionsByDate)

        val timetablesByDate = timetableResult.entities.associateByRecordDate()
        val nutritionsByDate = nutritionResult.entities.associateByRecordDate()

        return NotionDataResponse(
            startDate = startDate,
            endDate = endDate,
            fetched =
                NotionDataFetchedCount(
                    timetables = timetableResult.fetchedCount,
                    conditions = conditionResult.fetchedCount,
                    nutritions = nutritionResult.fetchedCount,
                    foods = nutritionResult.fetchedFoodCount,
                ),
            days =
                dates.map { date ->
                    DailyNotionDataResponse(
                        date = date,
                        timetable = timetablesByDate[date]?.toResponse(),
                        condition = conditionsByDate[date]?.toResponse(),
                        nutrition = nutritionsByDate[date]?.toResponse(),
                    )
                },
        )
    }

    private fun syncConditions(dates: List<LocalDate>): SyncResult<ConditionEntity> {
        val cached = conditionRepository.findAllByRecordDateIn(dates)
        val cachedByDate = cached.associateByRecordDate()
        val fetched =
            dates
                .filterNot { cachedByDate.containsKey(it) }
                .mapNotNull { date ->
                    fetchPageByTitle(notionProperties.conditionDatabaseId, date)
                        ?.let { conditionMapper.toEntity(it, date) }
                }
        val saved = conditionRepository.saveAll(fetched).toList()
        return SyncResult(cached + saved, saved.size)
    }

    private fun syncTimetables(
        dates: List<LocalDate>,
        conditionsByDate: Map<LocalDate, ConditionEntity>,
    ): SyncResult<TimetableEntity> {
        val cached = timetableRepository.findAllByRecordDateIn(dates)
        val cachedByDate = cached.associateByRecordDate()
        val fetched =
            dates
                .filterNot { cachedByDate.containsKey(it) }
                .mapNotNull { date ->
                    fetchPageByTitle(notionProperties.timetableDatabaseId, date)
                        ?.let { timetableMapper.toEntity(it, date) }
                        ?.also { it.condition = conditionsByDate[date] }
                }
        val saved = timetableRepository.saveAll(fetched).toList()
        return SyncResult(cached + saved, saved.size)
    }

    private fun syncNutritions(
        dates: List<LocalDate>,
        conditionsByDate: Map<LocalDate, ConditionEntity>,
    ): NutritionSyncResult {
        val cached = nutritionRepository.findAllByRecordDateIn(dates)
        val cachedByDate = cached.associateByRecordDate()
        var fetchedFoodCount = 0
        val fetched =
            dates
                .filterNot { cachedByDate.containsKey(it) }
                .mapNotNull { date ->
                    fetchPageByTitle(notionProperties.nutritionDatabaseId, date)
                        ?.let { nutritionMapper.toMapping(it, date) }
                        ?.also { mapping ->
                            mapping.entity.condition = conditionsByDate[date]
                            mapping.foodPageIdsByMealType.forEach { (mealType, foodPageIds) ->
                                foodPageIds.forEach { foodPageId ->
                                    val foodResult = findOrFetchFood(foodPageId)
                                    if (foodResult.fetched) fetchedFoodCount += 1
                                    mapping.entity.addFoodNutrition(foodResult.food, mealType)
                                }
                            }
                        }?.entity
                }
        val saved = nutritionRepository.saveAll(fetched).toList()
        return NutritionSyncResult(cached + saved, saved.size, fetchedFoodCount)
    }

    private fun findOrFetchFood(pageId: String): FoodFetchResult {
        val cached = foodRepository.findByNotionPageId(pageId)
        if (cached.isPresent) return FoodFetchResult(cached.get(), fetched = false)

        val fetched = foodMapper.toEntity(notionApiClient.fetchPageWithContent(pageId))
        return FoodFetchResult(foodRepository.save(fetched), fetched = true)
    }

    private fun fetchPageByTitle(
        databaseId: String,
        date: LocalDate,
    ) = notionApiClient
        .queryDatabaseByTitleEquals(
            databaseId = databaseId,
            titleProperty = notionProperties.titleProperty,
            titleEquals = date.format(titleFormatter),
        )?.let(notionApiClient::fetchPageWithContent)

    private fun <T : com.junseok.personal_data_ai.persistence.notion.entity.DatedNotionPageEntity> List<T>.associateByRecordDate(): Map<LocalDate, T> =
        mapNotNull { entity -> entity.recordDate?.let { it to entity } }.toMap()

    private data class SyncResult<T>(
        val entities: List<T>,
        val fetchedCount: Int,
    )

    private data class NutritionSyncResult(
        val entities: List<NutritionEntity>,
        val fetchedCount: Int,
        val fetchedFoodCount: Int,
    )

    private data class FoodFetchResult(
        val food: FoodEntity,
        val fetched: Boolean,
    )
}
