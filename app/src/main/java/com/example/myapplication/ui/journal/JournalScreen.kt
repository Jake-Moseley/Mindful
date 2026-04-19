package com.example.myapplication.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.JournalEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(navController: NavController) {

    //gives context for JournalViewModel
    val context = LocalContext.current
    val database = AppDB.getDatabase(context)
    val dao = database.journalDAO()
    val viewModel: JournalViewModel = viewModel(
        factory = JournalViewModelFactory(dao)
    )

    //pulls all entries in table
    //create selectedEntry variable to use and modify
    val allEntries by viewModel.journalEntries.collectAsState()
    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null)}

    //checks if selectedEntry exists, if not, go to beginning of table
    LaunchedEffect(allEntries) {
        if (selectedEntry == null && allEntries.isNotEmpty()) {
            selectedEntry = allEntries.first()
        }
    }

    //create showDialog bool
    var showDialog by remember { mutableStateOf(false) }
    //create showDialogComplete bool to use for complete confirmation
    var showDialogComplete by remember { mutableStateOf(false) }

    //get date as LocalDate! to pass in CreateEntry()
    val date = LocalDate.now()
    val currentEntry = allEntries.find {it.date == date.toString()}


    //new journal bar checks
    if (currentEntry == null) //if no current entry exists -> create entry
    {
        Scaffold(
            bottomBar = {
                BottomAppBar(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.CreateEntry(date) },  //on button click, create entry and pass date
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
        if (!currentEntry.complete) {   //if current entry exists but isn't complete -> edit entry'
            Scaffold(
                bottomBar = {
                    BottomAppBar(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { showDialogComplete = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text("Complete Entry")
                        }
                    }
                }
            ) {}
        }
        else {      //if current entry exists and is complete -> tell user to view from menu, showDialog = true
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
                            Text("Today's Entry Completed")
                        }

                    }
                }
            ) {}
        }
    }

    //checks showDialog, then displays message
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

    if (showDialogComplete) {
        AlertDialog(
            onDismissRequest = { showDialogComplete = false },
            title = { Text("Do you want to complete this entry?") },
            text = { Text("Complete entries cannot be edited anymore.")},
            confirmButton = {
                Button(onClick = { showDialogComplete = false }) {
                    Text("Complete Entry")
                }
            },
            dismissButton = {
                Button(onClick = { showDialogComplete = false}) {
                    Text("Not yet")
                }
            }
        )
    }

    //gets text from current entry to display when entry is clicked
    var loadedText by remember(selectedEntry) { mutableStateOf(selectedEntry?.content ?: "") }

    //main column with back button
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Journal") },
            navigationIcon = {
                IconButton(onClick = {
                    selectedEntry?.complete?.let {
                        if (!it) {
                            viewModel.SaveEntry(loadedText)
                        }
                    }
                    navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")      //add Alert to ask if entry is complete, if so, change bool in table
                }                                                                                //also will add demo options, as in no date restriction
            }
        )

        //journal row
        //dynamically changes depending on journal_entries table
        //formatter sets proper format and then date is parsed through to display proper format date
        val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

        LazyRow(modifier = Modifier.wrapContentSize(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(allEntries) { item ->
                ElevatedButton(
                    onClick = { selectedEntry = item },
                    shape = CircleShape
                ) {
                    Text(LocalDate.parse(item.date).format(formatter))
                }
            }
        }

        //checks if entry has been selected, opens entry text if clicked
        Box( modifier = Modifier.fillMaxSize(),
             contentAlignment = Alignment.Center)
        {
            selectedEntry?.let { entry ->
                val scroll = rememberScrollState()
                Column () {
                    OutlinedTextField(
                        value = loadedText,
                        onValueChange = {loadedText = it},
                        enabled = !entry.complete,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Justify),
                        modifier = Modifier.height(725.dp).width(375.dp).verticalScroll(scroll)
                    )
                }
            }
        }


    }
}



