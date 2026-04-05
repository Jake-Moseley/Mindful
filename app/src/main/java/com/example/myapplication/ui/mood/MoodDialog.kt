package com.example.myapplication.ui.mood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDialog(selectedDate: LocalDate?, onDismiss: () -> Unit, onMoodSelected: (MoodType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Your Mood!")
        },
        text = {
            Column {
                MoodType.values().forEach { mood ->
                    Text(
                        text = "${mood.emoji} ${mood.name}",
                        modifier = Modifier.fillMaxWidth().clickable { onMoodSelected(mood) }.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}