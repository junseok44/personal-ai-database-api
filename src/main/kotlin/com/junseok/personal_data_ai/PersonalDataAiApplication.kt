package com.junseok.personal_data_ai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PersonalDataAiApplication

fun main(args: Array<String>) {
	runApplication<PersonalDataAiApplication>(*args)
}
