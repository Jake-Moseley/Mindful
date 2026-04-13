package com.example.myapplication.ui.pet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun PetScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Virtual Pet Screen")
    }
}

*/
    Still working on it
    package com.example.myapplication.ui.pet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun PetScreen(
    navController: NavController,
    viewModel: PetViewModel = viewModel()
) {
    val pet by viewModel.petState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("🐾 Your Pet: ${pet.name}", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Text("Level: ${pet.level}")
        Text("XP: ${pet.experience}")
        Text("Happiness: ${pet.happiness}")

        Spacer(modifier = Modifier.height(30.dp))

        // --- ACTION BUTTONS ---
        Button(onClick = { viewModel.feedPet() }) {
            Text("Feed 🍖")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { viewModel.playWithPet() }) {
            Text("Play 🎾")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { viewModel.restPet() }) {
            Text("Rest 😴")
        }
    }
}

/*
