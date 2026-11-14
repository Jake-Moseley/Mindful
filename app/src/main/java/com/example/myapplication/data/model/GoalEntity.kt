package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Groups goals by area of life. Saved in the database by name, so entries can be reordered safely. */
enum class GoalCategory(val label: String) {
    MIND("Mind"),
    BODY("Body"),
    SOCIAL("Social"),
    SELF_CARE("Self-care");

    companion object {
        fun fromName(name: String): GoalCategory = entries.firstOrNull { it.name == name } ?: SELF_CARE
    }
}

/** A goal the user has set. Goals repeat every day. Removing a goal archives it so its history is kept. */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val emoji: String,
    val category: String,      // GoalCategory.name
    val createdDate: String,   // yyyy-MM-dd
    val isArchived: Boolean = false
)

/** One row per goal per day it was completed. A goal is "done today" if a row exists for today's date. */
@Entity(tableName = "goal_completions", primaryKeys = ["goalId", "date"])
data class GoalCompletion(
    val goalId: Int,
    val date: String           // yyyy-MM-dd
)