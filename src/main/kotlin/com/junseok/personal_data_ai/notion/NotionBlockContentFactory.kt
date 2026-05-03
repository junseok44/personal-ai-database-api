package com.junseok.personal_data_ai.notion

import org.springframework.stereotype.Component

@Component
class NotionBlockContentFactory {
    companion object {
        private const val NOTION_RICH_TEXT_MAX_LENGTH = 2000
    }

    fun propertyRichTextArray(content: NotionPagePropertyContent): List<Map<String, Any>> =
        when (content) {
            is NotionPagePropertyContent.Plain ->
                richTextObjects(content.text, "default")
            is NotionPagePropertyContent.Rich ->
                content.parts.flatMap { richTextObjects(it.text, it.color) }
        }

    fun createBulletedBlock(
        text: String,
        children: List<Map<String, Any>> = emptyList(),
    ): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "bulleted_list_item",
            "bulleted_list_item" to
                buildMap<String, Any> {
                    put("rich_text", plainRichTextObjects(text))
                    if (children.isNotEmpty()) {
                        put("children", children)
                    }
                },
        )

    fun createToggleBlock(
        text: String,
        children: List<Map<String, Any>> = emptyList(),
    ): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "toggle",
            "toggle" to
                buildMap<String, Any> {
                    put("rich_text", plainRichTextObjects(text))
                    if (children.isNotEmpty()) {
                        put("children", children)
                    }
                },
        )

    fun createParagraphBlock(text: String): Map<String, Any> =
        mapOf(
            "object" to "block",
            "type" to "paragraph",
            "paragraph" to
                mapOf(
                    "rich_text" to plainRichTextObjects(text),
                ),
        )

    private fun richTextObjects(
        text: String,
        color: String,
    ): List<Map<String, Any>> = splitRichTextContent(text).map { part -> richTextObject(part, color) }

    private fun richTextObject(
        text: String,
        color: String,
    ): Map<String, Any> =
        mapOf(
            "type" to "text",
            "text" to mapOf("content" to text),
            "annotations" to
                mapOf(
                    "bold" to false,
                    "italic" to false,
                    "strikethrough" to false,
                    "underline" to false,
                    "code" to false,
                    "color" to color,
                ),
        )

    private fun plainRichTextObjects(text: String): List<Map<String, Any>> =
        splitRichTextContent(text).map { part ->
            mapOf(
                "type" to "text",
                "text" to mapOf("content" to part),
            )
        }

    private fun splitRichTextContent(text: String): List<String> =
        if (text.isEmpty()) {
            listOf("")
        } else {
            text.chunked(NOTION_RICH_TEXT_MAX_LENGTH)
        }
}
