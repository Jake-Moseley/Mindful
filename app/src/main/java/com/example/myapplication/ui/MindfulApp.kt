package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.security.PasscodeManager
import com.example.myapplication.ui.goals.GoalsScreen
import com.example.myapplication.ui.goals.GoalsViewModel
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.insights.InsightsScreen
import com.example.myapplication.ui.insights.InsightsViewModel
import com.example.myapplication.ui.journal.JournalScreen
import com.example.myapplication.ui.mood.MoodScreen
import com.example.myapplication.ui.newnav.NewNavScreen
import com.example.myapplication.ui.quotes.QuoteScreen
import com.example.myapplication.ui.resources.ResourcesScreen
import com.example.myapplication.ui.security.PasscodeMode
import com.example.myapplication.ui.security.PasscodeScreen
import com.example.myapplication.ui.text.TextScreen

@Composable
fun MindfulApp() {
    val navController = rememberNavController()
    val goalsViewModel: GoalsViewModel = viewModel()
    val insightsViewModel: InsightsViewModel = viewModel()
    val context = LocalContext.current
    val passcodeManager = remember { PasscodeManager(context) }
    val isLocked = remember { mutableStateOf(passcodeManager.isPasscodeEnabled()) }

    // Goals are kept in memory by GoalsViewModel, so pass them to the pattern analysis when they change
    LaunchedEffect(goalsViewModel.goals) {
        insightsViewModel.updateGoals(goalsViewModel.goals)
    }

    val startRoute = if (isLocked.value) "unlock" else "home"

    NavHost(navController = navController, startDestination = startRoute) {
        composable("unlock") {
            PasscodeScreen(
                navController = navController,
                mode = PasscodeMode.UNLOCK,
                onSuccess = {
                    isLocked.value = false
                    navController.navigate("home") {
                        popUpTo("unlock") { inclusive = true }
                    }
                }
            )
        }
        composable("passcode_setup") {
            PasscodeScreen(
                navController = navController,
                mode = PasscodeMode.SETUP,
                onSuccess = {
                    isLocked.value = false
                    navController.navigate("home") {
                        popUpTo("passcode_setup") { inclusive = true }
                    }
                }
            )
        }
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
