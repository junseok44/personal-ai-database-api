package com.junseok.personal_data_ai

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "notion.api-key=test-api-key",
        "notion.database-id=test-database-id",
    ],
)
class PersonalDataAiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
