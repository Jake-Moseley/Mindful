package com.example.myapplication.ui.insights

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.ai.AnalysisConsent
import com.example.myapplication.data.ai.ConcernLevel
import com.example.myapplication.data.ai.PatternReport
import com.example.myapplication.ui.mood.MoodType
import java.time.LocalDate

private val ScreenBackground = Color(0xFFFFFDF9)
private val CardCream = Color(0xFFFBE6CD)
private val CardBlue = Color(0xFFDDF4F8)
private val CrisisRed = Color(0xFFFDE2E1)
private val CrisisRedDark = Color(0xFFB3261E)

fun levelColors(level: ConcernLevel): Pair<Color, Color> = when (level) {
    ConcernLevel.CRISIS -> CrisisRed to CrisisRedDark
    ConcernLevel.ELEVATED -> Color(0xFFF5CD9B) to Color(0xFF8A4B00)
    ConcernLevel.MILD -> Color(0xFFFBE4B9) to Color(0xFF6B5300)
    ConcernLevel.STEADY -> Color(0xFFD7F2DF) to Color(0xFF1E6B3A)
    ConcernLevel.NOT_ENOUGH_DATA -> CardBlue to Color.DarkGray
}

fun levelTitle(level: ConcernLevel): String = when (level) {
    ConcernLevel.CRISIS -> "Support is available now"
    ConcernLevel.ELEVATED -> "Worth checking in"
    ConcernLevel.MILD -> "A few tougher days"
    ConcernLevel.STEADY -> "Looking steady"
    ConcernLevel.NOT_ENOUGH_DATA -> "Getting to know you"
}

const val NON_CLINICAL_DISCLAIMER =
    "These are observations about patterns in what you've logged, not a medical diagnosis. " +
            "Mindful is a companion tool, not a healthcare provider."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(navController: NavController, viewModel: InsightsViewModel = viewModel()) {
    val report by viewModel.report.collectAsState()
    val consent by viewModel.consent.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val level by viewModel.displayLevel.collectAsState()
    val context = LocalContext.current

    // Run the AI analysis when the screen opens (only if allowed; cached if nothing changed).
    LaunchedEffect(consent, report) {
        if (consent == AnalysisConsent.AI_ALLOWED) viewModel.runAiAnalysis()
    }

    Column(modifier = Modifier.fillMaxSize().background(ScreenBackground)) {
        TopAppBar(
            title = { Text("Pattern Insights", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DisclaimerCard()

            // Crisis support is always shown when needed, even before the user picks a consent option.
            if (report.level == ConcernLevel.CRISIS) {
                CrisisCard(
                    onCall = { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:988"))) },
                    onText = { openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:988"))) },
                    onResources = { navController.navigate("resources") }
                )
            }

            if (consent == AnalysisConsent.NOT_ASKED) {
                ConsentCard(
                    onAllowAi = { viewModel.setConsent(AnalysisConsent.AI_ALLOWED) },
                    onDeviceOnly = { viewModel.setConsent(AnalysisConsent.ON_DEVICE_ONLY) }
                )
            } else {
                if (report.level != ConcernLevel.CRISIS) {
                    LevelCard(
                        level = level,
                        headline = report.headline,
                        onResources = { navController.navigate("resources") }
                    )
                }

                MoodTrendCard(report)

                val insight = aiState.insight
                if (consent == AnalysisConsent.AI_ALLOWED && (aiState.loading || insight != null)) {
                    AiSummaryCard(loading = aiState.loading, summary = insight?.summary)
                }
                aiState.message?.let { NoteText(it) }
                if (consent == AnalysisConsent.ON_DEVICE_ONLY) {
                    NoteText("AI analysis is off. You're seeing analysis done on your phone only.")
                }

                val observations = insight?.observations?.takeIf { it.isNotEmpty() } ?: report.observations
                val suggestions = insight?.suggestions?.takeIf { it.isNotEmpty() } ?: report.suggestions
                BulletCard(title = "What we noticed", items = observations, emptyText = "Nothing to show yet.")
                BulletCard(title = "Things you could try", items = suggestions, emptyText = "Nothing to suggest yet.")
                Text(
                    text = if (insight != null) "Generated by Google Gemini (AI) · Non-clinical guidance"
                    else "On-device analysis · Non-clinical guidance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (consent == AnalysisConsent.AI_ALLOWED) {
                        Button(
                            onClick = { viewModel.runAiAnalysis(force = true) },
                            enabled = !aiState.loading
                        ) { Text("Refresh analysis") }
                    }
                    OutlinedButton(onClick = { navController.navigate("resources") }) {
                        Text("Professional help")
                    }
                }

                PrivacyCard(
                    consent = consent,
                    onChange = { viewModel.setConsent(AnalysisConsent.NOT_ASKED) }
                )
            }

            InsightsTestTools() // TESTING ONLY: remove this line (and InsightsTestTools.kt) before release

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(10.dp))
            Text(NON_CLINICAL_DISCLAIMER, fontSize = 13.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun CrisisCard(onCall: () -> Unit, onText: () -> Unit, onResources: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CrisisRed),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = CrisisRedDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(levelTitle(ConcernLevel.CRISIS), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CrisisRedDark)
            }
            Text(
                "Some of your recent writing suggests you may be going through something really hard. " +
                        "You don't have to face it alone. The 988 Suicide & Crisis Lifeline is free, confidential and available 24/7.",
                color = Color.DarkGray
            )
            Text("If you're in immediate danger, call 911.", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = CrisisRedDark)
                ) { Text("Call 988") }
                OutlinedButton(onClick = onText) { Text("Text 988") }
            }
            TextButton(onClick = onResources) { Text("See professional help resources") }
        }
    }
}

@Composable
private fun ConsentCard(onAllowAi: () -> Unit, onDeviceOnly: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardCream),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.DarkGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Allow pattern analysis?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
            }
            Text(
                "Mindful can look at your mood logs, journal entries and goals to spot recurring patterns " +
                        "and suggest ways to feel better.",
                color = Color.DarkGray
            )
            Text("If you allow AI analysis, Mindful sends Google Gemini (through Firebase):", color = Color.DarkGray)
            Text(
                "• Your moods from the last 30 days\n" +
                        "• Up to 7 journal entries from the last 2 weeks, shortened\n" +
                        "• Today's goals and whether they're done",
                color = Color.DarkGray
            )
            Text(
                "Your name and account details are never sent. You can change this at any time.",
                color = Color.DarkGray
            )
            Button(onClick = onAllowAi, modifier = Modifier.fillMaxWidth()) { Text("Allow AI analysis") }
            OutlinedButton(onClick = onDeviceOnly, modifier = Modifier.fillMaxWidth()) {
                Text("On-device only (nothing is sent)")
            }
        }
    }
}

@Composable
private fun LevelCard(level: ConcernLevel, headline: String, onResources: () -> Unit) {
    val (background, accent) = levelColors(level)
    Card(colors = CardDefaults.cardColors(containerColor = background), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(levelTitle(level), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accent)
            Text(headline, color = Color.DarkGray)
            if (level == ConcernLevel.ELEVATED) {
                Button(
                    onClick = onResources,
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Find a professional") }
            }
        }
    }
}

// The last 14 days as a row of emoji, so trends over time are visible at a glance
@Composable
private fun MoodTrendCard(report: PatternReport) {
    val byDate: Map<LocalDate, MoodType> = report.recentMoods.associate { it.date to it.mood }
    val days = (13 downTo 0).map { report.generatedOn.minusDays(it.toLong()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your last 14 days", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            listOf(days.take(7), days.drop(7)).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { date -> DayCell(date, byDate[date]) }
                }
            }
            if (report.moodCounts.isNotEmpty()) {
                HorizontalDivider()
                Text("Last 30 days", fontSize = 13.sp, color = Color.Gray)
                MoodType.entries.forEach { mood ->
                    MoodCountBar(mood, report.moodCounts[mood] ?: 0, report.moodCounts.values.max())
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, mood: MoodType?) {
    val label = "${date.monthValue}/${date.dayOfMonth}: ${mood?.label ?: "not logged"}"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp).semantics { contentDescription = label }
    ) {
        Text(mood?.emoji ?: "·", fontSize = 22.sp, textAlign = TextAlign.Center, color = Color.LightGray)
        Text("${date.dayOfMonth}", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun MoodCountBar(mood: MoodType, count: Int, max: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${mood.emoji} ${mood.label}", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.width(110.dp))
        Box(modifier = Modifier.weight(1f).height(10.dp).background(Color(0xFFF1F1F1), RoundedCornerShape(5.dp))) {
            if (count > 0 && max > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(count.toFloat() / max)
                        .height(10.dp)
                        .background(Color(0xFF70DDF2), RoundedCornerShape(5.dp))
                )
            }
        }
        Text("$count", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 8.dp).width(24.dp))
    }
}

@Composable
private fun AiSummaryCard(loading: Boolean, summary: String?) {
    Card(colors = CardDefaults.cardColors(containerColor = CardCream), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.DarkGray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI summary", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Looking for patterns…", color = Color.DarkGray)
                }
            } else if (summary != null) {
                Text(summary, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun BulletCard(title: String, items: List<String>, emptyText: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            if (items.isEmpty()) {
                Text(emptyText, color = Color.Gray)
            } else {
                items.forEach { item ->
                    Row {
                        Text("•", color = Color.DarkGray, modifier = Modifier.padding(end = 8.dp))
                        Text(item, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(consent: AnalysisConsent, onChange: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FA)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.DarkGray)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (consent == AnalysisConsent.AI_ALLOWED) "AI analysis is on." else "AI analysis is off.",
                color = Color.DarkGray,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onChange) { Text("Change") }
        }
    }
}

@Composable
private fun NoteText(text: String) {
    Text(text, fontSize = 13.sp, color = Color.Gray)
}

private fun openIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {

    }
}