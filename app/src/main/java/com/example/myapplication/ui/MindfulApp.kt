package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.auth.AuthScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.journal.JournalScreen
import com.example.myapplication.ui.forum.ForumScreen
import com.example.myapplication.ui.mood.MoodScreen
import com.example.myapplication.ui.pet.PetScreen
import com.example.myapplication.ui.goals.GoalsScreen
import com.example.myapplication.ui.resources.ResourcesScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.ui.quotes.QuoteScreen


@Composable
fun MindfulApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") { AuthScreen(navController, authViewModel) }
        composable("home") { HomeScreen(navController, authViewModel) }
        composable("journal") { JournalScreen(navController) }
        composable("forum") { ForumScreen(navController) }
        composable("mood") { MoodScreen(navController) }
        composable("pet") { PetScreen(navController) }
        composable("goals") { GoalsScreen(navController) }
        composable("resources") { ResourcesScreen(navController) }
        composable(route = "quotes") {QuoteScreen(navController)}
    }
}
