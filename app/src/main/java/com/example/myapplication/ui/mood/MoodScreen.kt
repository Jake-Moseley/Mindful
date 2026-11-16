package com.example.myapplication.ui.mood

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.local.AppDB
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Colors shared by the Mood Tracker screens. Matches the home screen's warm pastel theme. */
object MoodColors {
    val Background = Color(0xFFFFFDF9)
    val Card = Color.White
    val TodayCard = Color(0xFFDDF4F8)
    val Accent = Color(0xFF70DDF2)
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextMuted = Color(0xFFB0B0B0)
    val Divider = Color(0xFFECE7E1)
    val EmptyCell = Color(0xFFF7F4F0)
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MoodScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dao = AppDB.getDatabase(context).moodDAO()
    val viewModel: MoodViewModel = viewModel(factory = MoodViewModelFactory(dao))

    val moodEntries by viewModel.moodEntries.collectAsState()
    val moodByDate: Map<LocalDate, MoodType> = moodEntries
        .mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { it to MoodType.fromInt(entry.mood) }
        }
        .toMap()

    val today = LocalDate.now()
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    var showPicker by remember { mutableStateOf(false) }
    val todayMood = moodByDate[today]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoodColors.Background)
    ) {
        TopAppBar(
            title = { Text("Mood Tracker", fontWeight = FontWeight.Bold, color = MoodColors.TextPrimary) },
            navigationIcon = {
                IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MoodColors.TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MoodColors.Background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TodayCard(
                today = today,
                mood = todayMood,
                onLogMood = { showPicker = true }
            )

            CalendarCard(
                month = displayedMonth,
                today = today,
                selectedDate = selectedDate,
                moodByDate = moodByDate,
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                onDateClick = { selectedDate = it }
            )

            selectedDate?.let { date ->
                SelectedDayCard(date = date, mood = moodByDate[date], isToday = date == today)
            }

            MonthSummaryCard(
                month = displayedMonth,
                moodByDate = moodByDate
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        MoodDialog(
            selectedDate = today,
            currentMood = todayMood,
            onDismiss = { showPicker = false },
            onMoodSelected = { mood ->
                viewModel.logMood(today, mood)
                selectedDate = today
                displayedMonth = YearMonth.from(today)
                showPicker = false
            }
        )
    }
}

// Today card

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TodayCard(today: LocalDate, mood: MoodType?, onLogMood: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MoodColors.TodayCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Today · ${today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))}",
                fontSize = 13.sp,
                color = MoodColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Text(text = mood?.emoji ?: "＋", fontSize = if (mood != null) 34.sp else 28.sp, color = MoodColors.TextMuted)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = mood?.label ?: "How are you feeling?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoodColors.TextPrimary
                    )
                    Text(
                        text = if (mood != null) "${mood.category.label} mood logged" else "You haven't logged a mood yet today.",
                        fontSize = 14.sp,
                        color = MoodColors.TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLogMood,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoodColors.TextPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(if (mood == null) "Log today's mood" else "Update today's mood", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


// Calendar


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate?,
    moodByDate: Map<LocalDate, MoodType>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MoodColors.Card),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoodColors.TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                )
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = MoodColors.TextPrimary)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = MoodColors.TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            DaysHeader()
            Spacer(modifier = Modifier.height(8.dp))

            // Weeks: plain rows (not a lazy grid) so the whole screen can scroll smoothly
            makeCalendar(month).chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { date ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (date != null) {
                                CalendarDayCell(
                                    date = date,
                                    mood = moodByDate[date],
                                    isToday = date == today,
                                    isSelected = date == selectedDate,
                                    isFuture = date.isAfter(today),
                                    onClick = { onDateClick(date) }
                                )
                            }
                        }
                    }
                    // Pad the last week so cells keep their width
                    repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            CategoryLegend()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun makeCalendar(currentMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = currentMonth.atDay(1)
    // Sunday-first calendar: Sunday = 0 ... Saturday = 6
    val firstDayIndex = firstOfMonth.dayOfWeek.value % 7
    val days = mutableListOf<LocalDate?>()
    repeat(firstDayIndex) { days.add(null) }
    for (day in 1..currentMonth.lengthOfMonth()) {
        days.add(currentMonth.atDay(day))
    }
    return days
}

@Composable
fun DaysHeader() {
    val days = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                color = MoodColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarDayCell(
    date: LocalDate,
    mood: MoodType?,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val fill = mood?.category?.color ?: MoodColors.EmptyCell
    val borderColor = when {
        isSelected -> MoodColors.TextPrimary
        isToday -> MoodColors.Accent
        else -> Color.Transparent
    }
    val description = buildString {
        append(date.format(DateTimeFormatter.ofPattern("MMMM d")))
        if (isToday) append(", today")
        append(": ")
        append(mood?.label ?: "no mood logged")
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(shape)
            .background(fill)
            .border(width = if (isSelected || isToday) 2.dp else 0.dp, color = borderColor, shape = shape)
            .clickable(enabled = !isFuture, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isFuture) MoodColors.TextMuted else MoodColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        )
        if (mood != null) {
            Text(
                text = mood.emoji,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun CategoryLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        MoodCategory.entries.forEach { category ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(category.color)
                        .border(1.dp, category.accent.copy(alpha = 0.4f), CircleShape)
                )
                Text(
                    text = category.label,
                    fontSize = 12.sp,
                    color = MoodColors.TextSecondary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Selected day
// ---------------------------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SelectedDayCard(date: LocalDate, mood: MoodType?, isToday: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MoodColors.Card),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(mood?.category?.color ?: MoodColors.EmptyCell)
            ) {
                Text(text = mood?.emoji ?: "–", fontSize = 24.sp, color = MoodColors.TextMuted)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (isToday) "Today" else date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    fontSize = 13.sp,
                    color = MoodColors.TextSecondary
                )
                Text(
                    text = mood?.label ?: "No mood logged",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoodColors.TextPrimary
                )
                if (mood != null) {
                    Text(
                        text = mood.category.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = mood.category.accent
                    )
                } else if (!isToday) {
                    Text(
                        text = "Past days can be viewed but not changed.",
                        fontSize = 12.sp,
                        color = MoodColors.TextMuted
                    )
                }
            }
        }
    }
}

// Month summary

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MonthSummaryCard(month: YearMonth, moodByDate: Map<LocalDate, MoodType>) {
    val monthMoods = moodByDate.filterKeys { YearMonth.from(it) == month }.values.toList()
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = MoodColors.Card),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$monthName at a glance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MoodColors.TextPrimary)

            if (monthMoods.isEmpty()) {
                Text(
                    text = "No moods logged this month yet.",
                    fontSize = 14.sp,
                    color = MoodColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "${monthMoods.size} ${if (monthMoods.size == 1) "day" else "days"} logged",
                    fontSize = 13.sp,
                    color = MoodColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Positive / Neutral / Difficult split as one stacked bar
                val byCategory = MoodCategory.entries.associateWith { cat -> monthMoods.count { it.category == cat } }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    MoodCategory.entries.forEach { cat ->
                        val count = byCategory[cat] ?: 0
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(count.toFloat())
                                    .height(12.dp)
                                    .background(cat.accent.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MoodCategory.entries.forEach { cat ->
                        val percent = ((byCategory[cat] ?: 0) * 100f / monthMoods.size).toInt()
                        Text("${cat.label} $percent%", fontSize = 12.sp, color = MoodColors.TextSecondary)
                    }
                }

                // Top 3 moods this month
                Spacer(modifier = Modifier.height(16.dp))
                Text("Most frequent", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MoodColors.TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                val topMoods = monthMoods.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)
                val maxCount = topMoods.first().value
                topMoods.forEach { (mood, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(mood.emoji, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                        Text(
                            text = mood.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MoodColors.TextPrimary,
                            modifier = Modifier.width(100.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MoodColors.EmptyCell)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(count.toFloat() / maxCount)
                                    .height(8.dp)
                                    .background(mood.category.accent.copy(alpha = 0.55f))
                            )
                        }
                        Text(
                            text = "$count",
                            fontSize = 13.sp,
                            color = MoodColors.TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
        }
    }
}