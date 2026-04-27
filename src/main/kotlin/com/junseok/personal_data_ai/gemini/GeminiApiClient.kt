package com.junseok.personal_data_ai.gemini

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.junseok.personal_data_ai.config.GeminiProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GeminiApiClient(
    private val geminiRestClient: RestClient,
    private val geminiProperties: GeminiProperties,
) {
    fun generateText(
        prompt: String,
        model: String = geminiProperties.model,
    ): String {
        val requestBody =
            mapOf(
                "contents" to
                    listOf(
                        mapOf(
                            "role" to "user",
                            "parts" to listOf(mapOf("text" to prompt)),
                        ),
                    ),
            )

        val response =
            geminiRestClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/models/{model}:generateContent")
                        .build(model)
                }.body(requestBody)
                .retrieve()
                .body(GeminiGenerateContentResponse::class.java)
                ?: throw IllegalStateException("Gemini generateContent failed: empty response")

        return response.candidates
            .asSequence()
            .flatMap { it.content.parts.asSequence() }
            .map { it.text }
            .firstOrNull { it.isNotBlank() }
            ?: throw IllegalStateException("Gemini generateContent failed: empty text candidate")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiCandidate(
    val content: GeminiContent = GeminiContent(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiPart(
    val text: String = "",
)
