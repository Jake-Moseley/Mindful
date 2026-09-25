package com.example.myapplication.ui.newnav

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.myapplication.ui.home.HomeScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.goals.GoalsViewModel

@Composable
fun NewNavScreen(navController: NavController, goalsViewModel: GoalsViewModel = viewModel()) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Home */ },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("goals") },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Goals") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("journal") },
                    icon = { Icon(Icons.Filled.Book, contentDescription = null) },
                    label = { Text("Journal") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("mood") },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("resources") },
                    icon = { Icon(Icons.Filled.ChatBubble, contentDescription = null) },
                    label = { Text("Resources") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeScreen(navController, goalsViewModel)
        }
    }
}
