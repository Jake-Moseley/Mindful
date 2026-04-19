package com.example.myapplication.ui.resources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
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
import com.example.myapplication.data.model.ResourceEntry
import com.example.myapplication.ui.journal.JournalViewModel
import com.example.myapplication.ui.journal.JournalViewModelFactory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(navController: NavController) {

    //gives context for JournalViewModel
    val context = LocalContext.current
    val database = AppDB.getDatabase(context)
    val dao = database.resourceDAO()
    val viewModel: ResourcesViewModel = viewModel(
        factory = ResourceViewModelFactory(dao)
    )

    //pulls all entries in table
    //create selectedEntry variable to use and modify
    val allEntries by viewModel.ResourceList.collectAsState()
    var selectedEntry by remember { mutableStateOf<ResourceEntry?>(null)}

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Resource") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                //generate demo entries
                actions = {
                    IconButton( onClick = { /*viewModel.DemoResources()*/ } ) {
                        Icon(Icons.Default.Build, contentDescription = "Demo")
                    }
                }
            )
        }
    ) {}

}
