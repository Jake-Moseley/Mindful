package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.journal.JournalScreen
import com.example.myapplication.ui.forum.ForumScreen
import com.example.myapplication.ui.mood.MoodScreen
import com.example.myapplication.ui.goals.GoalsScreen
import com.example.myapplication.ui.resources.ResourcesScreen
import com.example.myapplication.ui.quotes.QuoteScreen
import com.example.myapplication.ui.newnav.NewNavScreen
import com.example.myapplication.ui.text.TextScreen

@Composable
fun MindfulApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "newnav") {
        composable("home") { HomeScreen(navController) }
        composable("journal") { JournalScreen(navController) }
        composable("text") { TextScreen(navController) }
        composable("forum") { ForumScreen(navController) }
        composable("mood") { MoodScreen(navController) }
        composable("goals") { GoalsScreen(navController) }
        composable("resources") { ResourcesScreen(navController) }
        composable(route = "quotes") { QuoteScreen(navController) }
        composable("newnav") { NewNavScreen(navController) }
    }
}
