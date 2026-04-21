package com.junseok.personal_data_ai.reminder.timetable

import com.junseok.personal_data_ai.notion.RichTextPart

internal object DoneTodosReminderFormatter {
    private val VALID_TAG_VALUES = setOf(10, 15, 20, 25, 30, 35, 40, 45, 50)

    fun formatRichText(items: List<TimetableItemRequest>): List<RichTextPart> {
        val miscTitles = mutableListOf<String>()
        val scored = mutableListOf<ScoredLine>()
        for (item in items) {
            val title = item.title.trim()
            if (title.isBlank()) continue
            val tagInt = parseTagInt(item.tag)
            if (tagInt == null) {
                miscTitles.add(title)
            } else {
                scored.add(ScoredLine(title, tagInt / 10.0))
            }
        }
        val parts = mutableListOf<RichTextPart>()
        for (i in miscTitles.indices) {
            parts.add(RichTextPart("• ${miscTitles[i]}"))
            if (i < miscTitles.lastIndex) {
                parts.add(RichTextPart("\n"))
            }
        }
        if (miscTitles.isNotEmpty() && scored.isNotEmpty()) {
            parts.add(RichTextPart("\n\n"))
        }
        for (i in scored.indices) {
            val s = scored[i]
            val line = "• ${s.title} (${formatScore(s.score)}/5)"
            val color =
                when {
                    s.score < 3.0 -> "red"
                    s.score >= 4.0 -> "blue"
                    else -> "default"
                }
            parts.add(RichTextPart(line, color))
            if (i < scored.lastIndex) {
                parts.add(RichTextPart("\n"))
            }
        }
        return mergeAdjacentSameColor(parts)
    }

    private data class ScoredLine(
        val title: String,
        val score: Double,
    )

    private fun parseTagInt(tag: Any?): Int? {
        val raw = tagToRawString(tag) ?: return null
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val v = digits.toIntOrNull() ?: return null
        if (v !in VALID_TAG_VALUES) return null
        return v
    }

    private fun tagToRawString(tag: Any?): String? =
        when (tag) {
            null -> null
            is String -> tag.trim().takeIf { it.isNotEmpty() }
            is Number -> tag.toInt().toString()
            else -> null
        }

    private fun formatScore(score: Double): String {
        val whole = score.toInt()
        return if (score == whole.toDouble()) {
            whole.toString()
        } else {
            "%.1f".format(score)
        }
    }

    private fun mergeAdjacentSameColor(parts: List<RichTextPart>): List<RichTextPart> {
        if (parts.isEmpty()) return parts
        val out = mutableListOf<RichTextPart>()
        var cur = parts[0]
        for (i in 1 until parts.size) {
            val next = parts[i]
            cur =
                if (next.color == cur.color) {
                    RichTextPart(cur.text + next.text, cur.color)
                } else {
                    out.add(cur)
                    next
                }
        }
        out.add(cur)
        return out
    }
}

