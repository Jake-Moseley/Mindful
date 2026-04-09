package com.example.myapplication.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.JournalEntry
import java.time.LocalDate

//still need to add:
//Add entry functionality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(navController: NavController, viewModel: JournalViewModel = viewModel()) {

    val journalEntries = viewModel.journalEntries
    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null)}
    var showDialog by remember { mutableStateOf(false) }

    val date = LocalDate.now()
    val day = date.dayOfMonth
    val month = date.monthValue
    val year = date.year
    val currentDate = "$month/$day/$year"

    val currentEntry = journalEntries.find {it.date == "$month/$day/$year"}
    val newIndex = (journalEntries.maxByOrNull { it.id }?.id ?: 0) + 1

    //new journal bar checks
    if (currentEntry?.date == null) //if no current entry exists -> create entry
    {
        Scaffold(
            bottomBar = {
                BottomAppBar(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.CreateEntry(newIndex, currentDate) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Text("Add Entry")
                    }
                }
            }
        ) {}
    } else {
        if (currentEntry.complete == false) {   //if current entry exists but isn't complete -> edit entry
            Scaffold(
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text("Edit Entry")
                        }
                    }
                }
            ) {}
        }
        else {      //if current entry exists and is complete -> tell user to view from menu
                    //will add feature to bring up entry from this button later
            Scaffold(
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text("Entry Complete")
                        }

                    }
                }
            ) {}
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Entry already completed") },
            text = { Text("You can view your current entry from the menu above.")},
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Okay")
                }
            }
        )
    }

    //main column with back button
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {                                 //via MoodScreen.kt
        TopAppBar(
            title = { Text("Journal") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        // journal row
        //dynamically changes depending on journalEntries list
        JournalRow( items = journalEntries, onItemClick = { item -> selectedEntry = item })

        //checks if entry has been selected, opens entry text if clicked
        selectedEntry?.let { entry -> OpenText(entry)}
    }
}

//function that controls journal row of entries
@Composable
fun JournalRow( items: List<JournalEntry>, onItemClick: (JournalEntry) -> Unit)
{
    LazyRow(modifier = Modifier.wrapContentSize(Alignment.TopCenter),
        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(items) { item ->
            ElevatedButton(
                onClick = { onItemClick(item) },
                shape = CircleShape
            ) {
                Text(item.date)
            }
        }
    }
}

//function that controls the formatting for opened uneditable text
@Composable
fun OpenText(item : JournalEntry) {

    val scroll = rememberScrollState()
    Column () {
        Text(item.content,
            modifier = Modifier.height(650.dp).verticalScroll(scroll).offset(x = 11.dp),
            textAlign = TextAlign.Justify)
    }
}


