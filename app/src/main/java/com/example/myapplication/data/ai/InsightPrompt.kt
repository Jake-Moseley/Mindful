package com.example.myapplication.data.ai

import com.example.myapplication.data.model.Goal
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.ui.mood.MoodType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/// What Gemini sends back after parsing
data class AiInsight(
    val summary: String,
    val observations: List<String>,
    val suggestions: List<String>,
    /** "low", "moderate" or "high". Only used to raise the on-device level, never to lower it. */
    val concern: String
)

// Builds the Gemini prompt and parses its reply.

object InsightPrompt {

    const val MAX_JOURNAL_ENTRIES = 7
    const val MAX_JOURNAL_CHARS = 400
    private const val MAX_ITEMS = 5

    fun build(
        moods: List<MoodEntry>,
        journals: List<JournalEntry>,
        goals: List<Goal>,
        local: PatternReport,
        today: LocalDate = LocalDate.now()
    ): String {
        val moodLines = moods
            .mapNotNull { m -> parseDate(m.date)?.let { it to MoodType.fromInt(m.mood) } }
            .filter { (date, _) -> !date.isAfter(today) && ChronoUnit.DAYS.between(date, today) < 30 }
            .sortedBy { it.first }
            .joinToString("\n") { (date, mood) -> "- $date: ${mood.label}" }
            .ifEmpty { "(none logged)" }

        val journalLines = journals
            .mapNotNull { j -> parseDate(j.date)?.let { it to j.content.trim() } }
            .filter { (date, text) ->
                text.isNotEmpty() && !date.isAfter(today) && ChronoUnit.DAYS.between(date, today) < 14
            }
            .sortedByDescending { it.first }
            .take(MAX_JOURNAL_ENTRIES)
            .sortedBy { it.first }
            .joinToString("\n") { (date, text) -> "- $date: \"${text.take(MAX_JOURNAL_CHARS).replace("\n", " ")}\"" }
            .ifEmpty { "(no recent entries)" }

        val goalLines = goals
            .joinToString("\n") { "- ${it.title}: ${if (it.isCompleted) "completed" else "not completed"}" }
            .ifEmpty { "(no goals set)" }

        val localFindings = local.observations.joinToString("\n") { "- $it" }.ifEmpty { "(none)" }

        return """
            |You are the behavior pattern analysis feature of Mindful, a mental health companion app.
            |Look at the user's recent mood logs, journal entries and goals, and describe recurring
            |emotional or behavioral patterns in a warm, supportive, non-judgmental way.
            |
            |Rules you must follow:
            |- Never diagnose. Do not name or suggest any medical or mental health condition.
            |- Present everything as observations, not conclusions or medical advice.
            |- Suggestions must be small, practical self-care steps (sleep, movement, breathing,
            |  connecting with people, journaling, goals). Do not suggest medication or treatment plans.
            |- If the patterns look concerning, gently encourage talking with a mental health professional.
            |- Speak directly to the user as "you". Keep each item to one or two short sentences.
            |- Base everything only on the data below. Do not invent events.
            |
            |Today's date: $today
            |Mood scale used by the app: Happy, Sad, Frustrated, Tired.
            |
            |Mood logs (last 30 days):
            |$moodLines
            |
            |Journal entries (most recent, shortened):
            |$journalLines
            |
            |Today's goals:
            |$goalLines
            |
            |Patterns already found on the device:
            |$localFindings
            |
            |Reply with only a JSON object in exactly this shape:
            |{
            |  "summary": "2-3 sentence overview of the user's recent patterns",
            |  "observations": ["up to 4 specific patterns you noticed"],
            |  "suggestions": ["up to 4 small, practical suggestions"],
            |  "concernLevel": "low" | "moderate" | "high"
            |}
        """.trimMargin()
    }

    /** Parses Gemini's reply. Throws if it isn't the JSON shape we asked for. */
    fun parse(raw: String): AiInsight {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)
        val summary = json.optString("summary").trim()
        require(summary.isNotEmpty()) { "AI response had no summary" }
        return AiInsight(
            summary = summary,
            observations = json.optJSONArray("observations").toStringList(),
            suggestions = json.optJSONArray("suggestions").toStringList(),
            concern = json.optString("concernLevel", "low").trim().lowercase()
        )
    }

    /** Maps Gemini's concern onto our levels. The AI can raise the level to ELEVATED at most, never lower it. */
    fun combine(local: ConcernLevel, aiConcern: String): ConcernLevel {
        if (local == ConcernLevel.CRISIS || local == ConcernLevel.NOT_ENOUGH_DATA) return local
        val fromAi = when (aiConcern) {
            "high" -> ConcernLevel.ELEVATED
            "moderate" -> ConcernLevel.MILD
            else -> ConcernLevel.STEADY
        }
        return if (fromAi.rank > local.rank) fromAi else local
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .map { optString(it).trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_ITEMS)
    }

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}