package com.junseok.personal_data_ai.notion.sync

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class NotionPropertyReader(
    private val properties: Map<String, Any?>,
) {
    fun title(name: String): String? {
        val property = propertyMap(name)
        return richTextValue(property["title"])
    }

    fun richText(name: String): String? {
        val property = propertyMap(name)
        return richTextValue(property["rich_text"])
    }

    fun number(name: String): BigDecimal? {
        val value = propertyMap(name)["number"] ?: return null
        return when (value) {
            is Number -> BigDecimal.valueOf(value.toDouble())
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }

    fun date(name: String): LocalDate? {
        val date = propertyMap(name)["date"] as? Map<*, *> ?: return null
        val start = date["start"] as? String ?: return null
        return runCatching { LocalDate.parse(start.take(10)) }.getOrNull()
    }

    fun relationIds(name: String): List<String> {
        val relation = propertyMap(name)["relation"] as? List<*> ?: return emptyList()
        return relation.mapNotNull { (it as? Map<*, *>)?.get("id") as? String }
    }

    private fun propertyMap(name: String): Map<*, *> {
        return properties[name] as? Map<*, *> ?: emptyMap<Any, Any>()
    }

    private fun richTextValue(value: Any?): String? {
        val fragments = value as? List<*> ?: return null
        return fragments
            .mapNotNull { (it as? Map<*, *>)?.get("plain_text") as? String }
            .joinToString("")
            .trim()
            .takeIf { it.isNotBlank() }
    }
}

fun String?.toInstantOrNull(): Instant? =
    this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
