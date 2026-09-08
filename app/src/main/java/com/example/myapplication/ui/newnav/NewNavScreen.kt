package com.example.myapplication.ui.newnav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONArray
import java.util.Calendar

@Composable
fun NewNavScreen(navController: NavController) {
    val context = LocalContext.current
    val quote = remember {
        val json = context.assets.open("Quotes.json").bufferedReader().readText()
        val array = JSONArray(json)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        array.getString(dayOfYear % array.length())
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("journal") },
                    icon = { Icon(Icons.Filled.Book, contentDescription = null) },
                    label = { Text("Journal") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("mood") },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    label = { Text("Mood") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("goals") },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    label = { Text("Goals") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("resources") },
                    icon = { Icon(Icons.Filled.ChatBubble, contentDescription = null) },
                    label = { Text("Resources") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp).fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card( // AI Assistant Card
                modifier = Modifier.fillMaxWidth().clickable() { navController.navigate("assistant") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Column {
                        Text("Chat with your AI Assistant", fontWeight = FontWeight.Bold)
                        Text("Analyze your mood insights", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
