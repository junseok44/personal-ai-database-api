package com.junseok.personal_data_ai.persistence.notion.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant


// 식단 레코드와는 다대다 관계.
// 식단 레코드를 가져온다.

@Entity
@Table(name = "food")
class FoodEntity(
    id: Long? = null,
    notionPageId: String,
    notionLastEditedAt: Instant? = null,
    title: String,
    // 칼로리, 탄수화물, 단백질, 지방, 당, 나트륨, 포화지방 -> 숫자 컬럼을 가진다.
    @Column(name = "calories", precision = 10, scale = 2)
    var calories: BigDecimal? = null,
    @Column(name = "carbohydrates", precision = 10, scale = 2)
    var carbohydrates: BigDecimal? = null,
    @Column(name = "protein", precision = 10, scale = 2)
    var protein: BigDecimal? = null,
    @Column(name = "fat", precision = 10, scale = 2)
    var fat: BigDecimal? = null,
    @Column(name = "sugar", precision = 10, scale = 2)
    var sugar: BigDecimal? = null,
    @Column(name = "sodium", precision = 10, scale = 2)
    var sodium: BigDecimal? = null,
    @Column(name = "saturated_fat", precision = 10, scale = 2)
    var saturatedFat: BigDecimal? = null,
) : NotionPageEntity(
    id = id,
    notionPageId = notionPageId,
    notionLastEditedAt = notionLastEditedAt,
    title = title,
)

