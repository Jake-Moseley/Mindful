package com.example.myapplication.data.ai

import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.ui.mood.MoodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * How concerned the analysis is about the user's recent patterns.
 * Ordered from least to most concerning so levels.
 */
enum class ConcernLevel(val rank: Int) {
    NOT_ENOUGH_DATA(0),
    STEADY(1),
    MILD(2),
    ELEVATED(3),
    CRISIS(4)
}

// One logged mood
data class DailyMood(val date: LocalDate, val mood: MoodType)

// Results for the on-device analysis
data class PatternReport(
    val level: ConcernLevel,
    val headline: String,
    val observations: List<String>,
    val suggestions: List<String>,
    val recentMoods: List<DailyMood>,        // last 14 days
    val moodCounts: Map<MoodType, Int>,      // last 30 days
    val highRiskLanguageDetected: Boolean,
    val generatedOn: LocalDate
)

// On-device analysis, works entirely offline
object PatternAnalyzer {

    // Moods that lean negative. Tired is included because long runs of it are worth noticing.
    val NEGATIVE_MOODS = setOf(MoodType.SAD, MoodType.FRUSTRATED, MoodType.TIRED)

    // Phrases that indicate possible self-harm or severe distress
    val HIGH_RISK_PHRASES = listOf(
        "suicide", "suicidal", "kill myself", "killing myself", "end my life", "ending my life",
        "want to die", "wanna die", "wish i was dead", "wish i were dead", "better off dead",
        "better off without me", "no reason to live", "nothing to live for", "self harm",
        "self-harm", "hurt myself", "hurting myself", "cut myself", "cutting myself",
        "can't go on", "cant go on", "don't want to be here anymore", "dont want to be here anymore"
    )

    // Words that suggest deeper distress than a normal bad day
    private val DISTRESS_WORDS = setOf("hopeless", "worthless", "empty", "numb", "trapped", "helpless", "useless")

    private val NEGATIVE_WORDS = setOf(
        "sad", "anxious", "anxiety", "stressed", "stress", "tired", "exhausted", "lonely", "alone",
        "angry", "mad", "upset", "overwhelmed", "worried", "worry", "depressed", "cry", "cried",
        "crying", "scared", "afraid", "awful", "terrible", "miserable", "frustrated", "panic"
    ) + DISTRESS_WORDS

    private val POSITIVE_WORDS = setOf(
        "happy", "grateful", "thankful", "calm", "excited", "proud", "good", "great", "relaxed",
        "better", "fun", "love", "loved", "peaceful", "hopeful", "motivated", "rested", "glad", "enjoyed"
    )

    private const val MOOD_WINDOW_DAYS = 30L
    private const val JOURNAL_WINDOW_DAYS = 14L

    fun analyze(
        moods: List<MoodEntry>,
        journals: List<JournalEntry>,
        goals: List<Goal>,
        today: LocalDate = LocalDate.now()
    ): PatternReport {
        // ---- Moods ----
        val parsedMoods = moods
            .mapNotNull { entry -> parseDate(entry.date)?.let { DailyMood(it, MoodType.fromInt(entry.mood)) } }
            .filter { !it.date.isAfter(today) }
            .sortedBy { it.date }

        val last30 = parsedMoods.filter { daysAgo(it.date, today) < MOOD_WINDOW_DAYS }
        val last14 = parsedMoods.filter { daysAgo(it.date, today) < 14 }
        val last7 = parsedMoods.filter { daysAgo(it.date, today) < 7 }
        val prev7 = parsedMoods.filter { daysAgo(it.date, today) in 7..13 }

        val moodCounts = last30.groupingBy { it.mood }.eachCount()
        val negativeStreak = currentNegativeStreak(parsedMoods, today)
        val daysSinceLastLog = parsedMoods.lastOrNull()?.let { daysAgo(it.date, today) }
        val weekdayPattern = findWeekdayPattern(last30)

        // ---- Journal ----
        val recentJournals = journals
            .mapNotNull { entry -> parseDate(entry.date)?.let { it to entry.content } }
            .filter { (date, content) ->
                content.isNotBlank() && !date.isAfter(today) && daysAgo(date, today) < JOURNAL_WINDOW_DAYS
            }
        val journalText = recentJournals.joinToString(" ") { it.second }.lowercase(Locale.ROOT)
        val highRisk = containsHighRiskLanguage(journalText)
        val words = journalText.split(Regex("[^a-z']+")).filter { it.isNotBlank() }
        val negativeWordHits = words.filter { it in NEGATIVE_WORDS }
        val positiveWordCount = words.count { it in POSITIVE_WORDS }
        val distressCount = words.count { it in DISTRESS_WORDS }

        // ---- Goals ----
        val completedGoals = goals.count { it.isCompleted }

        // ---- Concern level ----
        val level = when {
            highRisk -> ConcernLevel.CRISIS
            last30.size < 3 && recentJournals.isEmpty() -> ConcernLevel.NOT_ENOUGH_DATA
            negativeStreak >= 5 ||
                    (last14.size >= 5 && negativeRatio(last14) >= 0.7) ||
                    distressCount >= 3 -> ConcernLevel.ELEVATED
            (last7.size >= 3 && negativeRatio(last7) >= 0.5) ||
                    (last7.size >= 2 && prev7.size >= 2 && negativeRatio(last7) - negativeRatio(prev7) >= 0.3) ||
                    negativeStreak >= 3 ||
                    negativeWordHits.size > positiveWordCount + 3 ||
                    distressCount >= 1 -> ConcernLevel.MILD
            else -> ConcernLevel.STEADY
        }

        // ---- Observations ----
        val observations = mutableListOf<String>()
        if (last30.isNotEmpty()) {
            val (topMood, topCount) = moodCounts.maxByOrNull { it.value }!!.toPair()
            observations += "You logged your mood ${times(last30.size)} in the last 30 days. " +
                    "Your most frequent mood was ${topMood.label} ($topCount)."
        }
        if (last7.size >= 2 && prev7.size >= 2) {
            val diff = negativeRatio(last7) - negativeRatio(prev7)
            observations += when {
                diff <= -0.2 -> "Your moods this week have been more positive than last week."
                diff >= 0.2 -> "Your moods this week have been less positive than last week."
                else -> "Your moods this week are about the same as last week."
            }
        }
        if (negativeStreak >= 3) {
            val latest = parsedMoods.last().mood.label
            observations += "Your last $negativeStreak logged days in a row were difficult ones (most recently $latest)."
        }
        weekdayPattern?.let { (day, mood) ->
            observations += "${mood.label} moods tend to show up on ${pluralDay(day)}."
        }
        if (daysSinceLastLog != null && daysSinceLastLog >= 3) {
            observations += "It's been $daysSinceLastLog days since you last logged your mood."
        }
        if (recentJournals.isNotEmpty()) {
            observations += "You wrote ${entries(recentJournals.size)} in the last 2 weeks."
            if (negativeWordHits.size > positiveWordCount) {
                val examples = negativeWordHits.groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }.take(2).joinToString(" and ") { "\"${it.key}\"" }
                observations += "Your recent writing uses more stress-related words (like $examples) than positive ones."
            } else if (positiveWordCount > 0 && positiveWordCount >= negativeWordHits.size) {
                observations += "Your recent writing has a mostly positive tone."
            }
        }
        if (goals.isNotEmpty()) {
            observations += "You've completed $completedGoals of ${goals.size} goals today."
        }

        return PatternReport(
            level = level,
            headline = headlineFor(level),
            observations = observations,
            suggestions = suggestionsFor(level, dominantNegativeMood(last14), goals),
            recentMoods = last14,
            moodCounts = moodCounts,
            highRiskLanguageDetected = highRisk,
            generatedOn = today
        )
    }

    fun containsHighRiskLanguage(text: String): Boolean {
        val normalized = text.lowercase(Locale.ROOT).replace('’', '\'')
        return HIGH_RISK_PHRASES.any { normalized.contains(it) }
    }

    fun isNegative(mood: MoodType): Boolean = mood in NEGATIVE_MOODS

    // ---------------------------------------------------------------------------------------

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

    private fun daysAgo(date: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(date, today)

    private fun negativeRatio(list: List<DailyMood>): Double =
        if (list.isEmpty()) 0.0 else list.count { isNegative(it.mood) }.toDouble() / list.size

    /**
     * Number of consecutive calendar days, ending at the most recent log, with a negative mood.
     * Only counts if the most recent log is from today or yesterday, so an old streak doesn't linger.
     */
    private fun currentNegativeStreak(sorted: List<DailyMood>, today: LocalDate): Int {
        val latest = sorted.lastOrNull() ?: return 0
        if (daysAgo(latest.date, today) > 1) return 0
        var streak = 0
        var expected = latest.date
        for (entry in sorted.asReversed()) {
            if (entry.date != expected || !isNegative(entry.mood)) break
            streak++
            expected = expected.minusDays(1)
        }
        return streak
    }

    /** A negative mood that repeatedly lands on the same weekday, e.g. "Tired on Mondays". */
    private fun findWeekdayPattern(moods: List<DailyMood>): Pair<DayOfWeek, MoodType>? {
        return moods
            .groupBy { it.date.dayOfWeek }
            .mapNotNull { (day, entries) ->
                val top = entries.filter { isNegative(it.mood) }
                    .groupingBy { it.mood }.eachCount()
                    .maxByOrNull { it.value } ?: return@mapNotNull null
                // Needs at least 2 occurrences and to make up most of that weekday's logs.
                if (top.value >= 2 && top.value.toDouble() / entries.size >= 0.66) Triple(day, top.key, top.value) else null
            }
            .maxByOrNull { it.third }
            ?.let { it.first to it.second }
    }

    private fun dominantNegativeMood(moods: List<DailyMood>): MoodType? =
        moods.filter { isNegative(it.mood) }.groupingBy { it.mood }.eachCount().maxByOrNull { it.value }?.key

    private fun headlineFor(level: ConcernLevel): String = when (level) {
        ConcernLevel.CRISIS ->
            "Some of your recent writing suggests you may be going through something really hard. You don't have to face it alone."
        ConcernLevel.ELEVATED ->
            "Your recent entries show a pattern of difficult days. Talking with a professional could really help."
        ConcernLevel.MILD ->
            "You've had some tougher days lately. A few small steps might help."
        ConcernLevel.STEADY ->
            "Your patterns look steady. Keep up your routines!"
        ConcernLevel.NOT_ENOUGH_DATA ->
            "Log your mood and journal for a few days to unlock pattern insights."
    }

    private fun suggestionsFor(level: ConcernLevel, dominant: MoodType?, goals: List<Goal>): List<String> {
        val list = mutableListOf<String>()
        when (level) {
            ConcernLevel.CRISIS -> {
                list += "Call or text 988 (Suicide & Crisis Lifeline) any time to talk with someone right now."
                list += "Reach out to someone you trust and let them know how you're feeling."
            }
            ConcernLevel.ELEVATED ->
                list += "Consider talking with a mental health professional. You can find options in Professional Help."
            ConcernLevel.NOT_ENOUGH_DATA -> {
                list += "Log your mood once a day from the Mood Tracker."
                list += "Write a short journal entry about your day."
            }
            else -> Unit
        }
        if (level != ConcernLevel.CRISIS) {
            when (dominant) {
                MoodType.TIRED -> {
                    list += "Try a consistent bedtime and put screens away 30 minutes before sleep."
                    list += "A 10-minute walk outside can help boost your energy."
                }
                MoodType.SAD -> {
                    list += "Reach out to someone you trust. Even a short message counts."
                    list += "Write down three small things that went okay today."
                }
                MoodType.FRUSTRATED -> {
                    list += "Try box breathing: breathe in for 4 seconds, hold for 4, out for 4, hold for 4."
                    list += "Take a short break away from whatever is frustrating you."
                }
                else -> Unit
            }
            if (goals.isNotEmpty() && goals.any { !it.isCompleted }) {
                list += "Pick one goal to finish today. Small wins add up."
            }
            if (level == ConcernLevel.STEADY) {
                list += "Keep journaling. It helps you notice what's working for you."
            }
        }
        return list.distinct().take(4)
    }

    private fun times(n: Int) = if (n == 1) "1 time" else "$n times"
    private fun entries(n: Int) = if (n == 1) "1 journal entry" else "$n journal entries"
    private fun pluralDay(day: DayOfWeek) = day.getDisplayName(TextStyle.FULL, Locale.US) + "s"
}