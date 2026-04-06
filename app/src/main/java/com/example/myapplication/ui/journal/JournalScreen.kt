package com.example.myapplication.ui.journal

import android.R.attr.entries
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.JournalEntry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(navController: NavController, viewModel: JournalViewModel = viewModel()) {

    //new journal bar
    Scaffold(
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
            ) {
                Button(onClick = {navController.navigate("text") }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp) ) {
                    Text("Add Entry")
                }
            }
        }
    ) {}

    //main column with back button
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) { //via MoodScreen.kt
        TopAppBar(
            title = { Text("Journal") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        Text("Journal Entries")
        Spacer(modifier = Modifier.height(12.dp))

        val journalEntries = viewModel.journalEntries

        // journal grid
        //dynamically changes depending on journalEntries list
        //will change layout later
        LazyVerticalGrid(
            modifier = Modifier.wrapContentSize(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = GridCells.Fixed(3)
        ) {
            items(journalEntries) { item ->
                ElevatedButton(
                    onClick = { navController.navigate("text") },
                    shape = CircleShape
                ) {
                    Text(item.date)
                }
            }
        }
    }
}


