package com.example.myapplication.ui.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.ResourceEntry

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
    val allResourceEntries by viewModel.ResourceList.collectAsState()
    var selectedResourceEntry by remember { mutableStateOf<ResourceEntry?>(null)}

    println(allResourceEntries.size)
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
                    IconButton( onClick = {  } ) {
                        Icon(Icons.Default.Build, contentDescription = "Demo")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (allResourceEntries.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {  items(allResourceEntries) { ResourceEntry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),

                ) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {

                        Image(painterResource(R.drawable.book_clipart), contentDescription = null, modifier = Modifier.size(165.dp))
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.fillMaxWidth().fillMaxSize()) {
                            Text(
                                ResourceEntry.name,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Justify)
                            Spacer(modifier = Modifier.size(3.dp))
                            Text(
                                ResourceEntry.phoneNum,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Justify,
                                fontSize = 12.sp)
                            Spacer(modifier = Modifier.padding(6.dp))
                            Text(
                                ResourceEntry.description,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Justify,
                                fontSize = 14.sp,
                                lineHeight = 18.sp)
                        }
                    }
                }
            }
            }
        }
        else {
            Text("Resources DB is Empty")
        }
    }
}
