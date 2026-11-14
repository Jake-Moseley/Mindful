package com.example.myapplication.data.repository

import com.example.myapplication.data.local.GoalDAO
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** A ready-made goal the user can add with one tap. */
data class GoalSuggestion(val title: String, val emoji: String, val category: GoalCategory)

/** Saves goals and daily completions in Room. Goals repeat every day; "done" means "completed today". */
class GoalRepository(private val dao: GoalDAO) {

    companion object {
        const val MAX_TITLE_LENGTH = 40
        const val MAX_ACTIVE_GOALS = 12

        /** Added automatically the first time the app runs (these were the original preset goals). */
        val STARTER_GOALS = listOf(
            GoalSuggestion("Walk 3,000 steps", "🚶", GoalCategory.BODY),
            GoalSuggestion("Meditate for 15 minutes", "🧘", GoalCategory.MIND),
            GoalSuggestion("Make your bed", "🛏️", GoalCategory.SELF_CARE)
        )

        /** One-tap suggestions shown on the Goals screen. */
        val SUGGESTIONS = STARTER_GOALS + listOf(
            GoalSuggestion("Drink 8 glasses of water", "💧", GoalCategory.BODY),
            GoalSuggestion("Journal for 5 minutes", "📝", GoalCategory.MIND),
            GoalSuggestion("Text a friend or family member", "💬", GoalCategory.SOCIAL),
            GoalSuggestion("Read for 20 minutes", "📖", GoalCategory.MIND),
            GoalSuggestion("Stretch for 10 minutes", "🤸", GoalCategory.BODY),
            GoalSuggestion("Spend 15 minutes outside", "🌳", GoalCategory.BODY),
            GoalSuggestion("Write down 3 things you're grateful for", "🙏", GoalCategory.MIND),
            GoalSuggestion("No screens 1 hour before bed", "📵", GoalCategory.SELF_CARE),
            GoalSuggestion("Eat a healthy meal", "🥗", GoalCategory.BODY),
            GoalSuggestion("Call someone you care about", "📞", GoalCategory.SOCIAL)
        )

        /**
         * Days in a row on which at least one goal was completed.
         * The streak counts back from today; if nothing is done yet today, it counts back from yesterday,
         * so the streak isn't lost just because the day isn't over.
         */
        fun calculateStreak(completionDates: List<String>, today: LocalDate = LocalDate.now()): Int {
            val days = completionDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
            var day = if (today in days) today else today.minusDays(1)
            var streak = 0
            while (day in days) {
                streak++
                day = day.minusDays(1)
            }
            return streak
        }

        /** Returns an error message for a goal title, or null if it's fine. */
        fun validateTitle(title: String, existing: List<Goal>, editingId: Int? = null): String? {
            val trimmed = title.trim()
            return when {
                trimmed.isEmpty() -> "Give your goal a name."
                trimmed.length > MAX_TITLE_LENGTH -> "Keep it under $MAX_TITLE_LENGTH characters."
                existing.any { it.id != editingId && it.title.equals(trimmed, ignoreCase = true) } ->
                    "You already have this goal."
                editingId == null && existing.size >= MAX_ACTIVE_GOALS ->
                    "You can have up to $MAX_ACTIVE_GOALS goals. Remove one to add another."
                else -> null
            }
        }
    }

    /** Today's active goals, each marked done or not done for [date]. */
    fun goalsFor(date: LocalDate): Flow<List<Goal>> =
        combine(dao.getActiveGoals(), dao.getCompletionsForDate(date.toString())) { goals, completions ->
            val doneIds = completions.map { it.goalId }.toSet()
            goals.map { entity ->
                Goal(
                    id = entity.id,
                    title = entity.title,
                    isCompleted = entity.id in doneIds,
                    emoji = entity.emoji,
                    category = GoalCategory.fromName(entity.category)
                )
            }
        }

    fun streak(today: LocalDate): Flow<Int> = dao.getCompletionDates().map { calculateStreak(it, today) }

    suspend fun addStarterGoalsIfNeeded(today: LocalDate) {
        if (dao.countAllGoals() == 0) {
            STARTER_GOALS.forEach { addGoal(it.title, it.emoji, it.category, today) }
        }
    }

    suspend fun addGoal(title: String, emoji: String, category: GoalCategory, today: LocalDate) {
        dao.insertGoal(
            GoalEntity(title = title.trim(), emoji = emoji, category = category.name, createdDate = today.toString())
        )
    }

    suspend fun updateGoal(id: Int, title: String, emoji: String, category: GoalCategory) {
        dao.updateGoal(id, title.trim(), emoji, category.name)
    }

    /** Hides the goal but keeps its past completions for the streak and Pattern Insights. */
    suspend fun archiveGoal(id: Int) = dao.archiveGoal(id)

    suspend fun setCompleted(goalId: Int, date: LocalDate, completed: Boolean) {
        if (completed) dao.insertCompletion(GoalCompletion(goalId, date.toString()))
        else dao.deleteCompletion(goalId, date.toString())
    }
}