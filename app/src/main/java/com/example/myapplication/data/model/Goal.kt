package com.example.myapplication.data.model

/** A goal as the screens and Pattern Insights see it: today's completion is already worked out. */
data class Goal(
    val id: Int = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
    val emoji: String = "🎯",
    val category: GoalCategory = GoalCategory.SELF_CARE
)