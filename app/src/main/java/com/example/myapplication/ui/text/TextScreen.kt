package com.example.myapplication.ui.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.ui.journal.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(navController: NavController, viewModel: JournalViewModel = viewModel()) {

    //main column and back button
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) { //via MoodScreen.kt
        TopAppBar(
            title = { Text("Journal") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }
}