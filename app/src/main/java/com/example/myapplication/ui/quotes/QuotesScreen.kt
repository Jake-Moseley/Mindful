package com.example.myapplication.ui.quotes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONArray
import java.util.Calendar
@Composable
fun QuoteScreen(navController: NavController)
 {
    val context = LocalContext.current
    val quote = remember {
        val json = context.assets.open("Quotes.json").bufferedReader().readText()
        val array = JSONArray(json)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        array.getString(dayOfYear % array.length())
    }
     Column(modifier = Modifier.fillMaxSize().padding(32.dp)
         , verticalArrangement = Arrangement.Center,
         horizontalAlignment = Alignment.CenterHorizontally
     )
     {
         Text(text = "Daily Quotes", style = MaterialTheme.typography.headlineMedium)
         Spacer(modifier = Modifier.height(24.dp))
         Text(text = "\"$quote\"", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
         Spacer(modifier = Modifier.height(24.dp))
         OutlinedButton(onClick = { navController.popBackStack()
         }) {
             Text("Back")
         }

     }

}