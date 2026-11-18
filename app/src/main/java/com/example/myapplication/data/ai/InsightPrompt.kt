package com.example.myapplication.data.ai

import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.ui.mood.MoodType
import org.json.JSONObject
import java.time.LocalDate

/** The Weekly Story as Gemini writes it. Every field is optional except the title. */
data class AiInsight(
    /** Short story title, e.g. "A slow start, a strong finish." Also shown on the home card. */
    val summary: String,
    /** A detailed 3-5 sentence recap that connects moods to what the user wrote. */
    val recap: String?,
    val felt: String?,
    val lifted: String?,
    val weighed: String?,
    val stepTitle: String?,
    val stepWhy: String?,
    val question: String?,
    /** "low", "moderate" or "high". Only used to raise the on-device level, never to lower it. */
    val concern: String
)

/**
 * Builds the Gemini prompt for the Weekly Story and parses its reply.
 *
 * Data minimization: only this week's moods, the facts the phone already found, and up to
 * 7 journal entries from this week (each cut to [MAX_JOURNAL_CHARS] characters) are sent.
 * No names, IDs or account details are included.
 */
object InsightPrompt {

    const val MAX_JOURNAL_ENTRIES = 7
    const val MAX_JOURNAL_CHARS = 600
    const val MAX_STEP_LENGTH = 40

    fun build(story: WeeklyStory, journals: List<JournalEntry>): String {
        val moodLines = story.days.joinToString("\n") { (date, mood) ->
            "- ${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} $date: ${mood?.label ?: "not logged"}"
        }

        val journalLines = journals
            .mapNotNull { j -> parseDate(j.date)?.let { it to j.content.trim() } }
            .filter { (date, text) -> text.isNotEmpty() && !date.isBefore(story.weekStart) && !date.isAfter(story.weekEnd) }
            .sortedBy { it.first }
            .take(MAX_JOURNAL_ENTRIES)
            .joinToString("\n") { (date, text) -> "- $date: \"${text.take(MAX_JOURNAL_CHARS).replace("\n", " ")}\"" }
            .ifEmpty { "(no journal entries this week)" }

        // What the phone found in each day's entry, so Gemini connects moods to the writing
        val momentLines = story.moments.joinToString("\n") { m ->
            val day = m.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val parts = buildList {
                add("mood: ${m.mood?.label?.lowercase() ?: "not logged"}")
                if (!m.hasEntry) add("no journal entry")
                // Only good feelings are passed along by name; hard ones are summarized gently
                if (m.positiveFeelings.isNotEmpty()) add("good feelings named: ${m.positiveFeelings.joinToString { it.word }}")
                if (m.heavy) add("some writing sounded very heavy")
                else if (m.gentleNote != null) add("a harder day")
                if (m.people.isNotEmpty()) add("people: ${m.people.joinToString()}")
                if (m.activities.isNotEmpty()) add("activities: ${m.activities.joinToString()}")
                if (m.topics.isNotEmpty()) add("topics: ${m.topics.joinToString { it.lowercase() }}")
            }
            "- $day: ${parts.joinToString("; ")}"
        }.ifEmpty { "(nothing logged)" }

        fun facts(chapter: StoryChapter) =
            chapter.evidence.joinToString("\n") { "  - ${it.text}" }.ifEmpty { "  (none)" }

        val moodScale = MoodType.entries.joinToString { "${it.label} (${it.category.label.lowercase()})" }

        return """
            |You write the "Weekly Story" for Mindful, a mental health companion app. It is a short,
            |warm recap of the user's week, like a thoughtful friend reflecting it back to them.
            |
            |Rules you must follow:
            |- Never diagnose or name any medical or mental health condition. This is not medical advice.
            |- Only use the data below. Do not invent events, people or numbers.
            |- Speak to the user as "you". Be warm, specific and honest, never cheesy or preachy.
            |- Be kind and uplifting. Never repeat the user's difficult words back to them
            |  (for example "hopeless", "empty", "worthless", "stressed", "anxious"). Describe hard
            |  moments gently instead ("a lot to carry", "running low on energy", "a tender few days").
            |- Pair every hard thing with something kind: what they did (checked in, wrote, kept going)
            |  or a bright spot from the week. Lead with strengths and end on something encouraging.
            |- Make it feel connected to what they wrote: tie each mood to what they wrote that day,
            |  name the people, activities and feelings they mentioned, and point out patterns
            |  (something that keeps coming up, or a mood that doesn't match their words).
            |- You may quote a few of their own words (under 12 words) in quotation marks, but only
            |  from good moments. Never quote anything hard, and never anything about self-harm.
            |- Even if they logged very little (one mood, one entry), write a specific recap from what
            |  is there. Don't pad it with generic advice, and don't pretend there's a pattern if there isn't.
            |- Keep it simple and a little fun, like a friendly highlight reel. Short sentences.
            |- Say each thing only once. The recap is the big picture; each chapter adds something new
            |  (a pattern, a theme, or a small tip), never the same fact again. Don't repeat counts.
            |- The recap is 2 sentences: the shape of the week, then one gentle contrast between what
            |  asked a lot of them and what lifted them. Each chapter is 1-2 sentences.
            |- In "lifted", name the theme behind their good days (e.g. "time with people you care
            |  about", "moving your body") rather than retelling a day.
            |- In "weighed", name what asked the most of them kindly and add one small, practical tip.
            |${if (story.heavyWriting) "- Some of their writing this week sounded very heavy. Don't describe it. In \"weighed\", gently\n|  say they don't have to carry it alone and that talking to someone they trust or a professional can help." else ""}
            |- The small step must be one concrete action they can do today, written as a goal
            |  (max $MAX_STEP_LENGTH characters, e.g. "Take a 10-minute walk after work").
            |- If the week looks very hard, gently mention that talking to a professional can help.
            |
            |Week: ${story.dateRange}
            |Moods the app offers: $moodScale
            |
            |Moods this week:
            |$moodLines
            |
            |Goals completed this week: ${story.goalsCompleted}
            |
            |Day by day (what the app found in each entry):
            |$momentLines
            |
            |The app's own recap:
            |${story.recap}
            |
            |Facts the app found:
            |How they felt:
            |${facts(story.felt)}
            |What lifted them:
            |${facts(story.lifted)}
            |What weighed on them:
            |${facts(story.weighed)}
            |
            |Journal entries this week (shortened):
            |$journalLines
            |
            |Reply with only a JSON object in exactly this shape:
            |{
            |  "title": "a 4-8 word title for the week, like a chapter heading",
            |  "recap": "a short, kind 2 sentence big-picture summary of their week",
            |  "felt": "how they felt this week",
            |  "lifted": "what seemed to lift them, or an empty string if nothing did",
            |  "weighed": "what asked a lot of them, said gently and paired with something they did, or an empty string",
            |  "smallStep": { "title": "one small action for today", "why": "one sentence on why, tied to their week" },
            |  "question": "one gentle reflection question for their journal",
            |  "concernLevel": "low" | "moderate" | "high"
            |}
        """.trimMargin()
    }

    /** Parses Gemini's reply. Throws if there's no usable title. */
    fun parse(raw: String): AiInsight {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)
        val title = json.optString("title").trim()
        require(title.isNotEmpty()) { "AI response had no title" }
        val step = json.optJSONObject("smallStep")
        return AiInsight(
            summary = title,
            recap = json.text("recap"),
            felt = json.text("felt"),
            lifted = json.text("lifted"),
            weighed = json.text("weighed"),
            stepTitle = step?.text("title")?.takeIf { it.length <= MAX_STEP_LENGTH },
            stepWhy = step?.text("why"),
            question = json.text("question"),
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

    private fun JSONObject.text(key: String): String? = optString(key).trim().takeIf { it.isNotEmpty() }

    private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}