package com.junseok.personal_data_ai.slack

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SlackApiClient(
    private val slackRestClient: RestClient,
) {
    fun postMessage(
        channelId: String,
        text: String,
    ) {
        val response =
            slackRestClient
                .post()
                .uri("/chat.postMessage")
                .body(
                    mapOf(
                        "channel" to channelId,
                        "text" to text,
                    ),
                ).retrieve()
                .body(SlackPostMessageResponse::class.java)
                ?: throw IllegalStateException("Slack postMessage failed: empty response")

        check(response.ok) { "Slack postMessage failed: ${response.error ?: "unknown error"}" }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SlackPostMessageResponse(
    val ok: Boolean = false,
    val error: String? = null,
)
