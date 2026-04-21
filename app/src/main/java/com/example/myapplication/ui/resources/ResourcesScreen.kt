package com.example.myapplication.ui.resources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    val query by viewModel.query.collectAsState()

    Scaffold(
        containerColor = Color.White,
        //top bar to hold back button
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    //search bar
                    Box(modifier = Modifier.fillMaxWidth().offset(x = -23.dp), contentAlignment = Alignment.Center) {
                        OutlinedTextField( value = query,
                            onValueChange = { viewModel.updateSearchQuery(it)},
                            placeholder = { Text("Search... ") },
                            textStyle = TextStyle(fontSize = 14.sp),
                            shape = CircleShape, singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth(0.8f) ) }
                },
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    ) { innerPadding ->
        //if db isn't empty output UI, db should never be empty
        Column(modifier = Modifier.padding(15.dp).background(Color.White)) {
            //checks if Resource table is empty
            if (allResourceEntries.isNotEmpty()) {
                //checks if search has been changed
                if (query == "") {
                    //if no change, display full list
                    UnfilteredList(allResourceEntries, innerPadding)
                }
                else {
                    //if change, display list with searched keywords
                    FilteredList(innerPadding, viewModel)
                }
            }
        }
    }
}

//function to intake description text, shorten it, and add "*... Tap Card For More"
@Composable
fun ShortenText(descText: String, maxChars: Int) {
    //checks if text needs to be shortened
    val isShortened = descText.length > maxChars

    //if so, shorten it using build annotated string
    val displayText = if (isShortened) {
        buildAnnotatedString {
            append(descText.take(maxChars))
            append("... \n")
            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                append("Tap Card For More")
            }
        }
    }
    //if not, simply output unchanged description text
    else {
        AnnotatedString(descText)
    }

    //display text
    Text(text = displayText, fontSize = 14.sp)
}

@Composable
fun UnfilteredList(allResourceEntries: List<ResourceEntry>, innerPadding: PaddingValues) {

    val backgroundColor = Color(0xFFF7F2FA)     //From GoalsScreen
    val borderColor = Color.LightGray
    var showDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<ResourceEntry?>(null)}

    LazyColumn(
        modifier = Modifier.padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {  items(allResourceEntries) { resourceEntry ->
        //display of each Resource Entry Card
        Card(
            onClick = { showDialog = true
                        selectedEntry = resourceEntry},
            colors = CardDefaults.cardColors(containerColor = backgroundColor),     //color, shape, and elevation modifications from goals page
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Image(painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(165.dp))

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.fillMaxWidth().fillMaxSize()) {

                    Text(
                        resourceEntry.name,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(3.dp))

                    Text(
                        resourceEntry.phoneNum,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontSize = 12.sp)

                    Spacer(modifier = Modifier.padding(4.dp))

                    ShortenText(resourceEntry.description, 100)
                }
            }
        }
    }
    }

    //checks showDialog, passes selectedEntry as entry
    if (showDialog == true) {
        selectedEntry?.let { entry ->
            InfoDialogBox(entry, showDialog, onDismiss = { showDialog = false })
        }
    }
}

@Composable
fun FilteredList(innerPadding: PaddingValues, viewModel: ResourcesViewModel) {

    //redeclaration of viewModel for new function
    val backgroundColor = Color(0xFFF7F2FA)     //From GoalsScreen
    val borderColor = Color.LightGray
    var showDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<ResourceEntry?>(null)}

    //Only creates filtered list if needed, updates as query changes
    val FilteredResourceEntries by viewModel.FilteredResourceList.collectAsState()

    //pulls all entries in table
    //create selectedEntry variable to use and modify
    LazyColumn(
        modifier = Modifier.padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {  items(FilteredResourceEntries) { resourceEntry ->
        //display of each Resource Entry Card
        Card(
            onClick = { showDialog = true
                        selectedEntry = resourceEntry},
            colors = CardDefaults.cardColors(containerColor = backgroundColor),     //color, shape, and elevation modifications from goals page
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Image(painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(165.dp))

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.fillMaxWidth().fillMaxSize()) {

                    Text(
                        resourceEntry.name,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(3.dp))

                    Text(
                        resourceEntry.phoneNum,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontSize = 12.sp)

                    Spacer(modifier = Modifier.padding(4.dp))

                    ShortenText(resourceEntry.description, 100)
                }
            }
        }
    }
    }
    //checks showDialog, passes selectedEntry as entry
    if (showDialog == true) {
        selectedEntry?.let { entry ->
            InfoDialogBox(entry, showDialog, onDismiss = { showDialog = false })
        }
    }
}

//Dialog pop-up to display more detailed information
@Composable
fun InfoDialogBox(resourceEntry: ResourceEntry, showDialog: Boolean, onDismiss: () -> Unit) {
    val backgroundColor = Color(0xFFF7F2FA)     //From GoalsScreen
    val borderColor = Color.LightGray
    if (showDialog) {
        Dialog(onDismissRequest = { onDismiss() }) {
            //pop-up card with formatting
            Card(
                colors = CardDefaults.cardColors(containerColor = backgroundColor),     //color, shape, and elevation modifications from goals page
                border = BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        resourceEntry.name,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontWeight = FontWeight.Bold

                    )

                    Spacer(modifier = Modifier.size(3.dp))

                    Text(
                        resourceEntry.phoneNum,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        resourceEntry.description,
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}