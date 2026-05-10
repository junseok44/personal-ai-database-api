package com.junseok.personal_data_ai.reminder.weather

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class WeatherAppendRequest(
    @field:Valid
    @field:NotEmpty
    val weather: List<WeatherItemRequest>,
)

data class WeatherItemRequest(
    @field:NotBlank
    val title: String,
)

data class WeatherAppendResponse(
    val pageId: String,
    val appendedCount: Int,
    val weatherCount: Int,
)
