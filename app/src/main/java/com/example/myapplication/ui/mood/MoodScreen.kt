package com.example.myapplication.ui.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class MoodType(val label: String, val emoji: String) {
    HAPPY("Happy", "\uD83D\uDE0A"),
    SAD("Sad", "\uD83D\uDE22"),
    FRUSTRATED("Frustrated", "\uD83D\uDE20")
}

data class MoodEntry(
    val date: String,
    val mood: MoodType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(navController: NavController) {
    val moodHistory = remember {
        mutableStateListOf(
            MoodEntry("04/03/2026", MoodType.HAPPY),
            MoodEntry("04/04/2026", MoodType.FRUSTRATED)
        )
    }
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mood Tracker") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text("Mood History")
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =  Arrangement.spacedBy(8.dp)
            ) {
                items(moodHistory.sortedByDescending { it.date }) {
                    entry -> OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.date)
                            Text("${entry.mood.emoji} ${entry.mood.label}")
                        }
                }
            }
        }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick =  { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log Your Mood!")
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false},
            title = { Text("What is your mood today?")},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoodType.entries.forEach { mood ->
                        OutlinedButton(
                            onClick = {
                                val today = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                    .format(Date())

                                moodHistory.removeAll { it.date == today }
                                moodHistory.add(MoodEntry(today, mood))

                                showDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${mood.emoji} ${mood.label}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

