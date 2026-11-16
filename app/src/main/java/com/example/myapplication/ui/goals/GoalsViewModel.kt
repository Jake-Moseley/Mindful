package com.example.myapplication.ui.goals

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.repository.GoalRepository
import com.example.myapplication.data.repository.GoalSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Daily goals, saved in Room. Shared by the Goals screen, the home widgets and Pattern Insights
 * (created once in MindfulApp).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GoalRepository(AppDB.getDatabase(application).goalDAO())

    /** The day goals are shown for. Updated by [refreshDay] so goals reset after midnight. */
    private val today = MutableStateFlow(LocalDate.now())

    /** Today's goals, each marked done or not. */
    var goals by mutableStateOf<List<Goal>>(emptyList())
        private set

    /** Days in a row with at least one goal completed. */
    var streak by mutableStateOf(0)
        private set

    val completedGoals: Int
        get() = goals.count { it.isCompleted }

    /** Suggested goals the user doesn't already have. */
    val suggestions: List<GoalSuggestion>
        get() = GoalRepository.SUGGESTIONS.filter { s -> goals.none { it.title.equals(s.title, ignoreCase = true) } }

    init {
        viewModelScope.launch {
            repository.addStarterGoalsIfNeeded(today.value)
        }
        viewModelScope.launch {
            today.flatMapLatest { repository.goalsFor(it) }.collect { goals = it }
        }
        viewModelScope.launch {
            today.flatMapLatest { repository.streak(it) }.collect { streak = it }
        }
    }

    /** Call when a screen opens so a new day starts with fresh goals. */
    fun refreshDay() {
        val now = LocalDate.now()
        if (now != today.value) today.value = now
    }

    fun toggleGoal(id: Int) {
        val goal = goals.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.setCompleted(id, today.value, !goal.isCompleted)
        }
    }

    /** Adds a goal. Returns an error message to show, or null if it was added. */
    fun addGoal(title: String, emoji: String, category: GoalCategory): String? {
        GoalRepository.validateTitle(title, goals)?.let { return it }
        viewModelScope.launch { repository.addGoal(title, emoji, category, today.value) }
        return null
    }

    /** Saves changes to a goal. Returns an error message to show, or null if it was saved. */
    fun updateGoal(id: Int, title: String, emoji: String, category: GoalCategory): String? {
        GoalRepository.validateTitle(title, goals, editingId = id)?.let { return it }
        viewModelScope.launch { repository.updateGoal(id, title, emoji, category) }
        return null
    }

    fun addSuggestion(suggestion: GoalSuggestion): String? =
        addGoal(suggestion.title, suggestion.emoji, suggestion.category)

    /** Removes the goal from the list. Its history is kept for the streak and Pattern Insights. */
    fun removeGoal(id: Int) {
        viewModelScope.launch { repository.archiveGoal(id) }
    }
}