package com.junseok.personal_data_ai.notion.sync

import com.junseok.personal_data_ai.notion.NotionBlock
import com.junseok.personal_data_ai.notion.NotionRichText

fun List<NotionBlock>.toPlainContent(): String? {
    return mapNotNull { it.toPlainText().takeIf(String::isNotBlank) }
        .joinToString("\n")
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun NotionBlock.toPlainText(): String {
    val richText =
        when (type) {
            "bulleted_list_item" -> bulleted_list_item?.rich_text
            "paragraph" -> paragraph?.rich_text
            "heading_1" -> heading_1?.rich_text
            "heading_2" -> heading_2?.rich_text
            "heading_3" -> heading_3?.rich_text
            "toggle" -> toggle?.rich_text
            else -> null
        }
    return richText.toPlainText()
}

private fun List<NotionRichText>?.toPlainText(): String =
    this?.joinToString("") { it.plain_text }.orEmpty().trim()
