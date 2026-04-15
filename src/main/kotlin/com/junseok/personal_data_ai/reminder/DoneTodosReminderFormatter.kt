package com.junseok.personal_data_ai.reminder

internal object DoneTodosReminderFormatter {
    private val VALID_TAG_VALUES = setOf(10, 15, 20, 25, 30, 35, 40, 45, 50)

    fun format(items: List<ReminderItemRequest>): String {
        val miscTitles = mutableListOf<String>()
        val scoredLines = mutableListOf<String>()
        for (item in items) {
            val title = item.title.trim()
            if (title.isBlank()) continue
            val tagInt = parseTagInt(item.tag)
            if (tagInt == null) {
                miscTitles.add(title)
            } else {
                val score = tagInt / 10.0
                scoredLines.add("• $title (${formatScore(score)}/5)")
            }
        }
        val miscBlock = miscTitles.joinToString("\n") { "• $it" }
        val scoredBlock = scoredLines.joinToString("\n")
        return when {
            miscBlock.isEmpty() && scoredBlock.isEmpty() -> ""
            miscBlock.isEmpty() -> scoredBlock
            scoredBlock.isEmpty() -> miscBlock
            else -> "$miscBlock\n\n$scoredBlock"
        }
    }

    /**
     * null·빈 값·유효하지 않은 숫자 → 자잘한 일(null).
     * 10~50 범위가 아니거나 5단위가 아니면 자잘한 일로 취급(제목만 상단 불렛).
     */
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
}
