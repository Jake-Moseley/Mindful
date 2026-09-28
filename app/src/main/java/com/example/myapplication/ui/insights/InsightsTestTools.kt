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
import com.example.myapplication.ui.mood.MoodType
import com.example.myapplication.ui.mood.MoodType.FRUSTRATED
import com.example.myapplication.ui.mood.MoodType.HAPPY
import com.example.myapplication.ui.mood.MoodType.SAD
import com.example.myapplication.ui.mood.MoodType.TIRED
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * TESTING ONLY: fills the database with sample moods and journal entries so the
 * AI Behavior Pattern Analysis can be tried without logging data for weeks.
 * The panel only shows in debug builds (when you press Run in Android Studio).
 * To remove it, delete this file and the one line that calls InsightsTestTools() in InsightsScreen.kt.
 */

/** A ready-made month of data. Each one is designed to trigger a different result. */
enum class TestScenario(val label: String, val expected: String) {
    STEADY("Steady month", "Green \"Looking steady\""),
    MONDAY_BLUES("Tired on Mondays", "Steady, plus a weekday pattern"),
    TOUGH_WEEK("Tough week", "Yellow \"A few tougher days\""),
    ELEVATED("Rough two weeks", "Orange \"Worth checking in\""),
    CRISIS("Crisis journal entry", "Red crisis card with 988")
}

/** Replaces ALL moods and journal entries with the chosen scenario. */
suspend fun loadTestScenario(db: AppDB, scenario: TestScenario, today: LocalDate = LocalDate.now()) {
    val moodDao = db.moodDAO()
    val journalDao = db.journalDAO()
    moodDao.deleteAll()
    journalDao.deleteAll()

    // moods[0] = today, moods[1] = yesterday, and so on
    val moods: List<MoodType> = when (scenario) {
        TestScenario.STEADY -> List(21) { i -> if (i % 7 == 3) TIRED else HAPPY }
        TestScenario.MONDAY_BLUES -> List(28) { i ->
            if (today.minusDays(i.toLong()).dayOfWeek == DayOfWeek.MONDAY) TIRED else HAPPY
        }
        TestScenario.TOUGH_WEEK -> listOf(SAD, TIRED, FRUSTRATED, HAPPY, SAD, TIRED, HAPPY) + List(14) { HAPPY }
        TestScenario.ELEVATED -> listOf(SAD, SAD, TIRED, SAD, FRUSTRATED, SAD, TIRED, SAD, HAPPY, SAD, TIRED, SAD, SAD, HAPPY)
        TestScenario.CRISIS -> listOf(SAD, SAD, TIRED, HAPPY, SAD, HAPPY, HAPPY)
    }
    moods.forEachIndexed { daysAgo, mood ->
        moodDao.insertMood(MoodEntry(date = today.minusDays(daysAgo.toLong()).toString(), mood = mood.ordinal))
    }

    // journals: days ago -> text
    val journals: List<Pair<Long, String>> = when (scenario) {
        TestScenario.STEADY, TestScenario.MONDAY_BLUES -> listOf(
            1L to "Good day. Went for a walk and felt calm and relaxed afterwards.",
            4L to "Grateful for dinner with friends tonight. Feeling happy."
        )
        TestScenario.TOUGH_WEEK -> listOf(
            1L to "Work has me stressed and anxious. Felt overwhelmed most of the day.",
            3L to "Tired and stressed again. Hard to focus.",
            6L to "Pretty good day overall."
        )
        TestScenario.ELEVATED -> listOf(
            1L to "Feeling hopeless about everything lately. Exhausted and lonely.",
            3L to "Another bad day. I feel worthless and empty.",
            6L to "Couldn't get out of bed. Everything feels hopeless."
        )
        TestScenario.CRISIS -> listOf(
            1L to "I don't know how much longer I can do this. Some days I feel like I want to die."
        )
    }
    journals.forEach { (daysAgo, text) ->
        journalDao.insertEntry(
            JournalEntry(date = today.minusDays(daysAgo).toString(), content = text, complete = true)
        )
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
                "Loading a scenario replaces ALL mood and journal data.",
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
                        status = "Cleared all moods and journal entries."
                    }
                }
            ) { Text("Clear all moods and journal entries") }

            status?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
        }
    }
}