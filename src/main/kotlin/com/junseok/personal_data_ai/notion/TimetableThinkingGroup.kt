package com.junseok.personal_data_ai.notion

data class TimetableThinkingGroup(
    val title: String,
    val children: List<String>,
    val aiFeedback: String? = null,
)

