package com.example.myapplication.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.Goal

class GoalsViewModel : ViewModel() {
    var goals by mutableStateOf(
        listOf(
            Goal(1, "Walk 3,000 Steps", false),
            Goal(2, "Meditate For 15 Minutes", false),
            Goal(3, "Make Your Bed", false)
        )
    )
        private set
    fun toggleGoal(id: Int) {
        goals = goals.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted)
            else it
        }
    }
    val completedGoals: Int
        get() = goals.count {it.isCompleted}
}
