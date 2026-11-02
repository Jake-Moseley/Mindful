package com.example.myapplication.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.DailyGoals
import com.example.myapplication.data.model.Goal
import java.time.LocalDate

class GoalsViewModel : ViewModel() {
    // Day the current goals were picked for, so they refresh if the app stays open past midnight
    private var goalsDate = LocalDate.now()

    var goals by mutableStateOf(DailyGoals.forDate(goalsDate))
        private set

    /** Swaps in a new set of goals if the day has changed. */
    fun refreshIfNewDay() {
        val today = LocalDate.now()
        if (today != goalsDate) {
            goalsDate = today
            goals = DailyGoals.forDate(today)
        }
    }

    fun toggleGoal(id: Int) {
        refreshIfNewDay()
        goals = goals.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted)
            else it
        }
    }

    val completedGoals: Int
        get() = goals.count { it.isCompleted }
}
