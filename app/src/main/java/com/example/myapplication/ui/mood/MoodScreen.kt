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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mood Tracker",
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp)

        )
        ElevatedButton(
            onClick = {
                selectedDate = LocalDate.now()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Log Your Mood!")
        }
        Text(
            text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
            color = Color.Black,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 38.dp)

        )
        DaysOfWeekHeader()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { date: LocalDate? ->
                if (date == null) {
                    Box(
                        modifier = Modifier.aspectRatio(1f)
                    )
                } else {
                    val mood = viewModel.getMoodForDate(date)

                    CalendarDayCell(
                        date = date,
                        mood = mood,
                        onClick = {
                        }
                    )
                }
            }
        }
        if (showDialog){
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
fun buildCalendarDays(currentMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()

    val firstDayIndex = when (firstOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

    val days = mutableListOf<LocalDate?>()

    repeat(firstDayIndex) {
        days.add(null)
    }

    for (day in 1..daysInMonth) {
        days.add(currentMonth.atDay(day))
    }

    return days
}

@Composable
fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
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