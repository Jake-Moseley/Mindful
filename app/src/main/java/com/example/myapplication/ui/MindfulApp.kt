package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.journal.JournalScreen
import com.example.myapplication.ui.mood.MoodScreen
import com.example.myapplication.ui.goals.GoalsScreen
import com.example.myapplication.ui.resources.ResourcesScreen
import com.example.myapplication.ui.quotes.QuoteScreen
import com.example.myapplication.ui.newnav.NewNavScreen
import com.example.myapplication.ui.text.TextScreen
import androidx.compose.runtime.LaunchedEffect
import com.example.myapplication.ui.insights.InsightsScreen
import com.example.myapplication.ui.insights.InsightsViewModel

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.goals.GoalsViewModel

@Composable
fun MindfulApp() {
    val navController = rememberNavController()
    val goalsViewModel: GoalsViewModel = viewModel()
    val insightsViewModel: InsightsViewModel = viewModel()

    // Goals are kept in memory by GoalsViewModel, so pass them to the pattern analysis when they change
    LaunchedEffect(goalsViewModel.goals) {
        insightsViewModel.updateGoals(goalsViewModel.goals)
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, goalsViewModel, insightsViewModel) }
        composable("journal") { JournalScreen(navController) }
        composable("text") { TextScreen(navController) }
        composable("mood") { MoodScreen(navController) }
        composable("goals") { GoalsScreen(navController, goalsViewModel) }
        composable("resources") { ResourcesScreen(navController) }
        composable(route = "quotes") { QuoteScreen(navController) }
        composable("newnav") { NewNavScreen(navController) }
        composable("insights") { InsightsScreen(navController, insightsViewModel) }
    }
}
