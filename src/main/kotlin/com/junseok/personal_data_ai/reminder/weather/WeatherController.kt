package com.junseok.personal_data_ai.reminder.weather

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/weather")
class WeatherController(
    private val weatherService: WeatherService,
) {
    @PostMapping("/reminders")
    fun appendWeather(
        @Valid @RequestBody request: WeatherAppendRequest,
    ): ResponseEntity<WeatherAppendResponse> = ResponseEntity.ok(weatherService.append(request))
}
