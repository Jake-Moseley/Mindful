package com.example.myapplication.ui.insights

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.repository.GoalRepository
import com.example.myapplication.ui.mood.MoodCategory
import com.example.myapplication.ui.mood.MoodType
import com.example.myapplication.ui.mood.MoodType.ANXIOUS
import com.example.myapplication.ui.mood.MoodType.CALM
import com.example.myapplication.ui.mood.MoodType.EXCITED
import com.example.myapplication.ui.mood.MoodType.FRUSTRATED
import com.example.myapplication.ui.mood.MoodType.GRATEFUL
import com.example.myapplication.ui.mood.MoodType.HAPPY
import com.example.myapplication.ui.mood.MoodType.OKAY
import com.example.myapplication.ui.mood.MoodType.OVERWHELMED
import com.example.myapplication.ui.mood.MoodType.SAD
import com.example.myapplication.ui.mood.MoodType.STRESSED
import com.example.myapplication.ui.mood.MoodType.TIRED
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * TESTING ONLY: fills the database with sample moods and journal entries so the
 * AI Behavior Pattern Analysis can be tried without logging data for weeks.
 * The panel only shows in debug builds (when you press Run in Android Studio).
 * To remove it, delete this file and the one line that calls InsightsTestTools() in InsightsScreen.kt.
 */

/** A ready-made month of data. Each one is designed to tell a different Weekly Story. */
enum class TestScenario(val label: String, val expected: String) {
    STEADY("Steady month", "\"A bright, steady week\" with goal and friends bright spots"),
    MONDAY_BLUES("Tired on Mondays", "\"Mondays seem to ask more of you\" in What asked a lot of you"),
    TOUGH_WEEK("Tough week at work", "Work on hard days, goals lifting your mood, a falling week"),
    ELEVATED("Rough two weeks", "Orange check-in with a \"Find a professional\" nudge"),
    CRISIS("Crisis journal entry", "Red crisis card with 988")
}

private val HARD_DAY_ENTRIES = listOf(
    "Work was a lot today. The deadline has me stressed and I couldn't switch off.",
    "Another long meeting at work. I'm exhausted and didn't sleep well.",
    "Couldn't focus at work, felt anxious most of the afternoon."
)
private val GOOD_DAY_ENTRIES = listOf(
    "Went for a walk after dinner and felt calm.",
    "Saw friends tonight and laughed a lot. Really needed that.",
    "Got outside in the sun for a while. Good, relaxed day."
)

/** Replaces ALL moods, journal entries and goal history with the chosen scenario. */
suspend fun loadTestScenario(db: AppDB, scenario: TestScenario, today: LocalDate = LocalDate.now()) {
    val moodDao = db.moodDAO()
    val journalDao = db.journalDAO()
    val goalDao = db.goalDAO()
    moodDao.deleteAll()
    journalDao.deleteAll()
    goalDao.deleteAllCompletions()

    // moods[0] = today, moods[1] = yesterday, and so on
    val moods: List<MoodType> = when (scenario) {
        TestScenario.STEADY -> List(21) { i -> listOf(HAPPY, CALM, GRATEFUL, TIRED, HAPPY, EXCITED, CALM)[i % 7] }
        TestScenario.MONDAY_BLUES -> List(28) { i ->
            if (today.minusDays(i.toLong()).dayOfWeek == DayOfWeek.MONDAY) TIRED
            else listOf(HAPPY, CALM, OKAY, GRATEFUL)[i % 4]
        }
        TestScenario.TOUGH_WEEK ->
            listOf(STRESSED, TIRED, ANXIOUS, CALM, OVERWHELMED, HAPPY, HAPPY) +
                    List(14) { i -> listOf(HAPPY, CALM, STRESSED, GRATEFUL, HAPPY, TIRED, CALM)[i % 7] }
        TestScenario.ELEVATED -> listOf(SAD, SAD, TIRED, SAD, FRUSTRATED, SAD, TIRED, SAD, HAPPY, SAD, TIRED, SAD, SAD, HAPPY)
        TestScenario.CRISIS -> listOf(SAD, SAD, TIRED, HAPPY, SAD, HAPPY, HAPPY)
    }
    moods.forEachIndexed { daysAgo, mood ->
        moodDao.insertMood(MoodEntry(date = today.minusDays(daysAgo.toLong()).toString(), mood = mood.ordinal))
    }

    // Journal entries: days ago -> text. Most scenarios write on 2 of every 3 days, matching the day's mood.
    val journals: List<Pair<Long, String>> = when (scenario) {
        TestScenario.ELEVATED -> listOf(
            1L to "Feeling hopeless about everything lately. Exhausted and lonely.",
            3L to "Another bad day at work. I feel worthless and empty.",
            6L to "Couldn't get out of bed. Everything feels hopeless."
        )
        TestScenario.CRISIS -> listOf(
            1L to "I don't know how much longer I can do this. Some days I feel like I want to die."
        )
        TestScenario.MONDAY_BLUES -> moods.mapIndexedNotNull { i, mood ->
            when {
                mood == TIRED -> i.toLong() to "Monday again. Back to work and I slept badly, so tired."
                i % 3 != 2 -> i.toLong() to GOOD_DAY_ENTRIES[i % GOOD_DAY_ENTRIES.size]
                else -> null
            }
        }
        else -> moods.mapIndexedNotNull { i, mood ->
            if (i % 3 == 2 || mood.category == MoodCategory.NEUTRAL) null
            else i.toLong() to (if (mood.category == MoodCategory.DIFFICULT) HARD_DAY_ENTRIES else GOOD_DAY_ENTRIES)[i % 3]
        }
    }
    journals.forEach { (daysAgo, text) ->
        journalDao.insertEntry(
            JournalEntry(date = today.minusDays(daysAgo).toString(), content = text, complete = daysAgo > 0)
        )
    }

    // Goal history: goals were completed on good days, so the story can connect goals and mood
    val goalRepository = GoalRepository(goalDao)
    goalRepository.addStarterGoalsIfNeeded(today)
    val goalId = goalDao.getActiveGoals().first().firstOrNull()?.id ?: return
    moods.forEachIndexed { daysAgo, mood ->
        if (mood.category == MoodCategory.POSITIVE && scenario != TestScenario.CRISIS) {
            goalDao.insertCompletion(GoalCompletion(goalId, today.minusDays(daysAgo.toLong()).toString()))
        }
    }
}

/** Saves as today's journal entry, replacing today's text if an entry already exists. */
suspend fun saveTestJournal(db: AppDB, text: String, today: LocalDate = LocalDate.now()) {
    val dao = db.journalDAO()
    val date = today.toString()
    if (dao.getJournalByDate(date) != null) {
        dao.updateEntry(text, date)
    } else {
        dao.insertEntry(JournalEntry(date = date, content = text, complete = false))
    }
}

suspend fun clearTestData(db: AppDB) {
    db.moodDAO().deleteAll()
    db.journalDAO().deleteAll()
    db.goalDAO().deleteAllCompletions()
}

/** The testing panel shown at the bottom of the Insights screen in debug builds. */
@Composable
fun InsightsTestTools() {
    val context = LocalContext.current
    val isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebugBuild) return

    val db = remember { AppDB.getDatabase(context) }
    val scope = rememberCoroutineScope()
    var journalText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Testing tools (debug builds only)", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Text(
                "Loading a scenario replaces ALL moods, journal entries and goal history.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            TestScenario.entries.forEach { scenario ->
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loadTestScenario(db, scenario)
                            status = "Loaded \"${scenario.label}\". Expect: ${scenario.expected}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(scenario.label) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Quick journal entry (saved as today's entry)", fontSize = 13.sp, color = Color.DarkGray)
            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                placeholder = { Text("Type a test journal entry…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            OutlinedButton(
                onClick = {
                    val text = journalText.trim()
                    if (text.isNotEmpty()) {
                        scope.launch {
                            saveTestJournal(db, text)
                            status = "Saved as today's journal entry."
                            journalText = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save journal entry") }

            TextButton(
                onClick = {
                    scope.launch {
                        clearTestData(db)
                        status = "Cleared moods, journal entries and goal history."
                    }
                }
            ) { Text("Clear moods, journal entries and goal history") }

            status?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}