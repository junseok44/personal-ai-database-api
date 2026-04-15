package com.junseok.personal_data_ai.notion

/** 한 줄(또는 연속 구간)에 적용할 Notion rich_text 색. API 값과 동일. */
data class RichTextPart(
    val text: String,
    val color: String = "default",
)

sealed class NotionPagePropertyContent {
    data class Plain(
        val text: String,
    ) : NotionPagePropertyContent()

    data class Rich(
        val parts: List<RichTextPart>,
    ) : NotionPagePropertyContent()
}
