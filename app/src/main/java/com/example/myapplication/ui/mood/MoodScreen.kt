package com.example.myapplication.ui.mood

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MoodScreen(
    navController: NavHostController,
    viewModel: MoodViewModel = viewModel()
) {
    val currentMonth = YearMonth.now()
    val days = buildCalendarDays(currentMonth)
    var showDialog by remember{ mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // First initial column that has houses the mood tracker title and log your mood button
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Mood Tracker",
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp) // modify to change location of title
        )
        ElevatedButton(
            onClick = {
                selectedDate = LocalDate.now()
                showDialog = true // Pops up the mood logging functionality
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp) //fills button from side to side
        ) {
            Text("Log Your Mood!") // Text for the button
        }
        Text( // calendar header
            text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}", // Month and Year above calendar
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 38.dp)
        )
        DaysOfWeekHeader()
        LazyVerticalGrid(
            columns = GridCells.Fixed(7), // 7 for each day of week
            modifier = Modifier.fillMaxWidth(), // fills entire screen
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { date: LocalDate? -> if (date == null) {
                    Box(
                        modifier = Modifier.aspectRatio(1f) // gives us our square shape for calendar
                    )
                } else {
                    val mood = viewModel.getMoodForDate(date) // grabbing mood from viewmodel
                    CalendarDayCell(
                        date = date, // day of week
                        mood = mood, // mood for that day
                        onClick = {
                            // makes the cell clickable, will come back to this to add mood desc
                        }
                    )
                }
            }
        }
        if (showDialog){ // mood logging logic
            MoodDialog(
                selectedDate = selectedDate,
                onDismiss = { showDialog = false },
                onMoodSelected = {mood -> viewModel.logMood(selectedDate, mood)
                    showDialog = false }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun buildCalendarDays(currentMonth: YearMonth): List<LocalDate?> { // build calendar logic
    val firstOfMonth = currentMonth.atDay(1) // first day of month
    val daysInMonth = currentMonth.lengthOfMonth() // how long month is

    val firstDayIndex = when (firstOfMonth.dayOfWeek) { // which day is the first?
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

    val days = mutableListOf<LocalDate?>() // list for days

    repeat(firstDayIndex) { // null for days before the 1st day of month
        days.add(null)
    }

    for (day in 1..daysInMonth) {
        days.add(currentMonth.atDay(day)) // every day in month
    }

    return days // finished list!
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") // Simple header, can change if needed

    LazyVerticalGrid(
        columns = GridCells.Fixed(7), // seven days
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daysOfWeek) { day ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = day,
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarDayCell(
    date: LocalDate,
    mood: MoodType?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, Color.DarkGray)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (mood != null) {
            Text(
                text = mood.emoji,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}