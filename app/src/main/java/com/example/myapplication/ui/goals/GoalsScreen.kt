package com.example.myapplication.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(navController: NavController, viewModel: GoalsViewModel = viewModel()) {
    val goals = viewModel.goals
    val completedGoals = goals.count { it.isCompleted }
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoalIndicatorBar(
            completedGoals = viewModel.completedGoals,
            totalGoals = goals.size
        )
        Spacer(modifier = Modifier.height(24.dp))
        goals.forEach { goal -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = goal.isCompleted,
                onCheckedChange = {
                    viewModel.toggleGoal(goal.id)
                }
            )
            Text(text = goal.title)
        } }
    }
}

@Composable
fun GoalIndicatorBar(completedGoals: Int, totalGoals: Int, modifier: Modifier = Modifier) {
    val progress = if (totalGoals > 0) completedGoals / totalGoals.toFloat() else 0f
    Box(
        modifier = modifier.size(120.dp), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = {progress},
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp
        )
        Text(
            text = "$completedGoals/$totalGoals",
            style = MaterialTheme.typography.titleMedium
        )
    }

}