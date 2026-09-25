package com.example.myapplication.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.ui.home.components.*
import com.example.myapplication.ui.mood.MoodType
import com.example.myapplication.ui.mood.MoodViewModel
import com.example.myapplication.ui.mood.MoodViewModelFactory
import org.json.JSONArray
import java.time.LocalDate
import java.util.Calendar

import com.example.myapplication.ui.goals.GoalsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController, goalsViewModel: GoalsViewModel = viewModel()) {
    val context = LocalContext.current
    
    // Setup ViewModel for Mood
    val db = AppDB.getDatabase(context)
    val dao = db.moodDAO()
    val viewModel: MoodViewModel = viewModel(
        factory = MoodViewModelFactory(dao)
    )
    val moodEntries by viewModel.moodEntries.collectAsState()
    
    // Find today's mood
    val todayStr = LocalDate.now().toString()
    val todayMoodEntry = moodEntries.find { it.date == todayStr }
    val todayMood = todayMoodEntry?.let { MoodType.fromInt(it.mood) }

    // Dynamic Quote
    val quote = remember {
        try {
            val json = context.assets.open("Quotes.json").bufferedReader().readText()
            val array = JSONArray(json)
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            array.getString(dayOfYear % array.length())
        } catch (_: Exception) {
            "Small steps every day lead to big changes."
        }
    }
    
    // Dynamic Greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning \uD83D\uDC4B"
        hour < 17 -> "Good afternoon \uD83D\uDC4B"
        else -> "Good evening \uD83D\uDC4B"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF9)) // Very light warm background
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = greeting,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$quote\"",
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF70DDF2)), // Cyan profile circle
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.DarkGray)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Widgets Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                MoodWidgetCard(
                    mood = todayMood, 
                    onClick = { navController.navigate("mood") }
                )
            }
            item {
                GoalsWidgetCard(
                    completedGoals = goalsViewModel.completedGoals,
                    totalGoals = goalsViewModel.goals.size,
                    onClick = { navController.navigate("goals") }
                )
            }
            item {
                StreakWidgetCard() // good widget not functional yet not sure if we want to track this based off of success from putting in
                // passcode (not re implemented yet) or we could base this off of succesful user interaction of the daily mood idk
            }
            item {
                SleepWidgetCard() // temp card not sure what to do or what the intended look is here but good for not
                // will be used in the demo and will talk with prof on what the best use of this screen should be
                // maybe expanding the AI to take up almost two cards worth then having 4 under idk
            }
            item {
                HydrationWidgetCard() // temp card not sure what we actually want to do here yet
            }
            item {
                AiCoachWidgetCard() // place holder static for now until AI screen is built
            }
        }
    }
}
