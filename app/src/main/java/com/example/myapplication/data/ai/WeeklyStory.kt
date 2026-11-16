package com.example.myapplication.data.ai

import com.example.myapplication.data.model.GoalCategory
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.ui.mood.MoodCategory
import com.example.myapplication.ui.mood.MoodType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** A fact taken straight from the user's own data, shown under a chapter so every claim has evidence. */
data class Evidence(val emoji: String, val text: String)

/** One chapter of the story: a short narrative plus the evidence behind it. */
data class StoryChapter(val narrative: String, val evidence: List<Evidence>)

/** A small, concrete action that can be added to today's goals with one tap. */
data class SmallStep(val title: String, val emoji: String, val category: GoalCategory, val why: String)

/**
 * The Weekly Story: a recap of one 7-day week, built entirely on the phone.
 * Gemini (when the user allows it) rewrites the narratives, but the evidence always comes from here.
 */
data class WeeklyStory(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val isCurrentWeek: Boolean,
    val hasData: Boolean,
    val title: String,
    val days: List<Pair<LocalDate, MoodType?>>,   // 7 days, oldest first
    val daysLogged: Int,
    val goalsCompleted: Int,
    val journalEntries: Int,
    val categorySplit: Map<MoodCategory, Int>,
    val topMoods: List<Pair<MoodType, Int>>,
    val felt: StoryChapter,
    val lifted: StoryChapter,
    val weighed: StoryChapter,
    val smallSteps: List<SmallStep>,
    val reflectionQuestion: String
) {
    val dateRange: String
        get() {
            val f = DateTimeFormatter.ofPattern("MMM d", Locale.US)
            return "${weekStart.format(f)} – ${weekEnd.format(f)}"
        }
}

object WeeklyStoryBuilder {

    /** Life topics we look for in journal entries, with the words that point to each one. */
    private val TOPICS: Map<String, Set<String>> = mapOf(
        "Work" to setOf("work", "job", "boss", "meeting", "meetings", "shift", "office", "coworker", "coworkers", "deadline", "deadlines"),
        "School" to setOf("school", "class", "classes", "exam", "exams", "test", "homework", "professor", "assignment", "assignments", "study", "studying", "grades"),
        "Sleep" to setOf("sleep", "slept", "insomnia", "nap", "bedtime", "awake"),
        "Family" to setOf("family", "mom", "dad", "mother", "father", "parents", "sister", "brother", "kids"),
        "Friends" to setOf("friend", "friends", "hangout", "hung"),
        "Exercise" to setOf("gym", "run", "ran", "walk", "walked", "workout", "exercise", "hike", "yoga", "swim"),
        "Money" to setOf("money", "bills", "rent", "pay", "paycheck", "debt", "budget"),
        "Health" to setOf("sick", "doctor", "pain", "headache", "illness", "fever"),
        "Relationships" to setOf("partner", "boyfriend", "girlfriend", "husband", "wife", "date", "breakup"),
        "Nature" to setOf("outside", "outdoors", "park", "sun", "sunshine", "nature", "beach")
    )

    private val TOPIC_EMOJI = mapOf(
        "Work" to "💼", "School" to "📚", "Sleep" to "😴", "Family" to "🏡", "Friends" to "🫂",
        "Exercise" to "🏃", "Money" to "💵", "Health" to "🩺", "Relationships" to "💞", "Nature" to "🌳"
    )

    private const val LOOKBACK_DAYS = 30L

    /**
     * @param weekEnd the last day of the week to tell the story of (today for the current week)
     */
    fun build(
        moods: List<MoodEntry>,
        journals: List<JournalEntry>,
        completions: List<GoalCompletion>,
        weekEnd: LocalDate,
        today: LocalDate = LocalDate.now(),
        sleepByDate: Map<LocalDate, Double> = emptyMap()   // hours slept, keyed by the day the user woke up
    ): WeeklyStory {
        val weekStart = weekEnd.minusDays(6)
        val moodByDate: Map<LocalDate, MoodType> = moods
            .mapNotNull { m -> parse(m.date)?.let { it to MoodType.fromInt(m.mood) } }
            .filter { !it.first.isAfter(today) }
            .toMap()
        val goalsByDate: Map<LocalDate, Int> = completions
            .mapNotNull { parse(it.date) }
            .groupingBy { it }.eachCount()
        val journalByDate: Map<LocalDate, String> = journals
            .mapNotNull { j -> parse(j.date)?.let { it to j.content } }
            .filter { it.second.isNotBlank() }
            .toMap()

        val weekDates = (0L..6L).map { weekStart.plusDays(it) }
        val days = weekDates.map { it to moodByDate[it] }
        val weekMoods = days.mapNotNull { it.second }
        val prevWeekMoods = (7L..13L).mapNotNull { moodByDate[weekEnd.minusDays(it)] }
        val goalsThisWeek = weekDates.sumOf { goalsByDate[it] ?: 0 }
        val journalsThisWeek = weekDates.count { it in journalByDate }

        val split = MoodCategory.entries.associateWith { cat -> weekMoods.count { it.category == cat } }
        val topMoods = weekMoods.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(3).map { it.key to it.value }

        // Window for patterns that need more than a week of data
        val windowStart = weekEnd.minusDays(LOOKBACK_DAYS - 1)
        val windowDates = generateSequence(windowStart) { it.plusDays(1) }.takeWhile { !it.isAfter(weekEnd) }.toList()

        val shape = weekShape(weekMoods = days.map { it.second })
        val hasData = weekMoods.isNotEmpty() || journalsThisWeek > 0

        val felt = buildFelt(weekMoods, prevWeekMoods, topMoods, split, weekDates.size)
        val topicLinks = topicLinks(windowDates, moodByDate, journalByDate)
        val lifted = buildLifted(windowDates, moodByDate, goalsByDate, topicLinks, days)
        val sleepLink = sleepLink(windowDates, moodByDate, sleepByDate)
        val weighed = buildWeighed(windowDates, moodByDate, topicLinks, days, sleepLink)

        val dominantDifficult = weekMoods.filter { it.category == MoodCategory.DIFFICULT }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val heaviestTopic = topicLinks.filter { it.difficultMentions > it.positiveMentions }
            .maxByOrNull { it.difficultMentions }?.topic

        return WeeklyStory(
            weekStart = weekStart,
            weekEnd = weekEnd,
            isCurrentWeek = !weekEnd.isBefore(today),
            hasData = hasData,
            title = titleFor(shape, weekMoods),
            days = days,
            daysLogged = weekMoods.size,
            goalsCompleted = goalsThisWeek,
            journalEntries = journalsThisWeek,
            categorySplit = split,
            topMoods = topMoods,
            felt = felt,
            lifted = lifted,
            weighed = weighed,
            smallSteps = smallStepsFor(dominantDifficult, heaviestTopic, sleepLink != null),
            reflectionQuestion = questionFor(shape, heaviestTopic, bestDay(days))
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 1: How you felt
    // ---------------------------------------------------------------------------------------------

    private fun buildFelt(
        weekMoods: List<MoodType>,
        prevWeekMoods: List<MoodType>,
        topMoods: List<Pair<MoodType, Int>>,
        split: Map<MoodCategory, Int>,
        weekLength: Int
    ): StoryChapter {
        if (weekMoods.isEmpty()) {
            return StoryChapter("No moods were logged this week. Logging once a day helps your story come together.", emptyList())
        }
        val evidence = mutableListOf<Evidence>()
        evidence += Evidence("📅", "Logged ${weekMoods.size} of $weekLength days")

        val positive = split[MoodCategory.POSITIVE] ?: 0
        val difficult = split[MoodCategory.DIFFICULT] ?: 0
        val dominantCategory = when {
            positive > weekMoods.size / 2 -> MoodCategory.POSITIVE
            difficult > weekMoods.size / 2 -> MoodCategory.DIFFICULT
            else -> null
        }
        val mostly = when (dominantCategory) {
            MoodCategory.POSITIVE -> "Most of your days felt positive"
            MoodCategory.DIFFICULT -> "Most of your days felt difficult"
            else -> "Your days were a mix of feelings"
        }
        // The mood to highlight: the most common one within the dominant group, and only if it repeated
        val highlight = weekMoods
            .filter { dominantCategory == null || it.category == dominantCategory }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }
            ?.takeIf { it.value >= 2 }
        highlight?.let { (mood, count) -> evidence += Evidence(mood.emoji, "${mood.label} ${times(count)} this week") }
        var narrative = "$mostly${if (highlight != null) ", with ${highlight.key.label.lowercase()} showing up most" else ""}."

        if (prevWeekMoods.size >= 2) {
            val diff = positiveRate(weekMoods) - positiveRate(prevWeekMoods)
            when {
                diff >= 0.2 -> {
                    narrative += " That's brighter than the week before."
                    evidence += Evidence("📈", "More positive days than the week before")
                }
                diff <= -0.2 -> {
                    narrative += " That's a bit heavier than the week before."
                    evidence += Evidence("📉", "Fewer positive days than the week before")
                }
            }
        }
        return StoryChapter(narrative, evidence)
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 2: What lifted you
    // ---------------------------------------------------------------------------------------------

    private fun buildLifted(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        goalsByDate: Map<LocalDate, Int>,
        topics: List<TopicLink>,
        days: List<Pair<LocalDate, MoodType?>>
    ): StoryChapter {
        val evidence = mutableListOf<Evidence>()
        val sentences = mutableListOf<String>()

        // Goals and mood: are goal days happier days?
        val logged = windowDates.filter { it in moodByDate }
        val goalDays = logged.filter { (goalsByDate[it] ?: 0) > 0 }
        val otherDays = logged.filter { (goalsByDate[it] ?: 0) == 0 }
        if (goalDays.size >= 3 && otherDays.size >= 3) {
            val withGoals = positiveRate(goalDays.map { moodByDate.getValue(it) })
            val without = positiveRate(otherDays.map { moodByDate.getValue(it) })
            if (withGoals - without >= 0.2) {
                evidence += Evidence("🎯", "Positive on ${pct(withGoals)} of goal days vs ${pct(without)} of other days")
                sentences += "Finishing goals seems to lift you: days you completed one felt noticeably better."
            }
        }

        // Topics that show up on good days
        topics.filter { it.positiveMentions >= 2 && it.positiveMentions > it.difficultMentions }
            .sortedByDescending { it.positiveMentions }
            .take(2)
            .forEachIndexed { i, link ->
                evidence += Evidence(TOPIC_EMOJI[link.topic] ?: "✨", "\"${link.topic}\" came up on ${link.positiveMentions} of your good days")
                if (i == 0) sentences += "\"${link.topic}\" keeps coming up on your good days."
            }

        // Best day this week
        bestDay(days)?.let { (date, mood) ->
            evidence += Evidence(mood.emoji, "Brightest day: ${dayName(date)} (${mood.label})")
            if (sentences.isEmpty()) sentences += "${dayName(date)} stood out as a bright spot."
        }

        val narrative = if (sentences.isEmpty()) {
            "Bright spots will show up here as you log more moods, goals and journal entries."
        } else sentences.take(2).joinToString(" ")
        return StoryChapter(narrative, evidence)
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 3: What weighed on you
    // ---------------------------------------------------------------------------------------------

    private fun buildWeighed(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        topics: List<TopicLink>,
        days: List<Pair<LocalDate, MoodType?>>,
        sleep: SleepLink?
    ): StoryChapter {
        val evidence = mutableListOf<Evidence>()
        val sentences = mutableListOf<String>()

        // Short nights and the day after (from Health Connect)
        if (sleep != null) {
            evidence += Evidence(
                "😴",
                "Difficult on ${pct(sleep.shortDifficultRate)} of days after under ${SHORT_SLEEP_HOURS.toInt()}h of sleep " +
                        "vs ${pct(sleep.restedDifficultRate)} after ${RESTED_SLEEP_HOURS.toInt()}h or more"
            )
            sentences += if (sleep.shortDifficultRate >= 0.5) {
                "On nights under ${SHORT_SLEEP_HOURS.toInt()} hours, your next day was usually difficult."
            } else {
                "Days after short nights tended to be harder for you."
            }
        }

        // Topics that show up on difficult days
        topics.filter { it.difficultMentions >= 2 && it.difficultMentions > it.positiveMentions }
            .sortedByDescending { it.difficultMentions }
            .take(2)
            .forEachIndexed { i, link ->
                evidence += Evidence(
                    TOPIC_EMOJI[link.topic] ?: "☁️",
                    "\"${link.topic}\" came up in ${link.difficultMentions} of ${link.difficultEntries} entries on difficult days"
                )
                if (i == 0) sentences += "\"${link.topic}\" came up a lot on your harder days."
            }

        // Weekday pattern over the lookback window
        windowDates.filter { it in moodByDate }
            .groupBy { it.dayOfWeek }
            .mapNotNull { (day, dates) ->
                val difficult = dates.map { moodByDate.getValue(it) }.filter { it.category == MoodCategory.DIFFICULT }
                if (difficult.size >= 2 && difficult.size.toDouble() / dates.size >= 0.66) Triple(day, difficult, dates.size) else null
            }
            .maxByOrNull { it.second.size }
            ?.let { (day, difficult, total) ->
                val common = difficult.groupingBy { it }.eachCount().maxBy { it.value }.key
                evidence += Evidence(common.emoji, "${difficult.size} of your last $total ${plural(day)} felt difficult")
                sentences += "${plural(day)} tend to be harder for you."
            }

        // Difficult days in a row this week
        val streak = longestDifficultRun(days)
        if (streak >= 3) {
            evidence += Evidence("🌧️", "$streak difficult days in a row")
            if (sentences.isEmpty()) sentences += "There was a stretch of $streak tough days in a row."
        }

        val narrative = if (sentences.isEmpty()) "Nothing stood out as weighing on you this week. 🌤️"
        else sentences.take(2).joinToString(" ")
        return StoryChapter(narrative, evidence)
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 4: Looking ahead
    // ---------------------------------------------------------------------------------------------

    private fun smallStepsFor(dominantDifficult: MoodType?, heaviestTopic: String?, shortSleepHurts: Boolean): List<SmallStep> {
        val steps = mutableListOf<SmallStep>()
        if (shortSleepHurts) {
            steps += SmallStep("Get to bed 30 minutes earlier", "😴", GoalCategory.SELF_CARE,
                "Your sleep data shows harder days after short nights.")
        }
        when (heaviestTopic) {
            "Work", "School" -> steps += SmallStep("Take a 10-minute break outside", "🌳", GoalCategory.BODY,
                "A short break away from $heaviestTopic can reset a heavy day.")
            "Sleep" -> steps += SmallStep("No screens 1 hour before bed", "📵", GoalCategory.SELF_CARE,
                "Sleep came up on hard days; a calmer evening can help.")
            "Money" -> steps += SmallStep("Spend 10 minutes planning your budget", "💵", GoalCategory.SELF_CARE,
                "A small plan can make money worries feel more manageable.")
            "Family", "Relationships", "Friends" -> steps += SmallStep("Write down one thing you need", "📝", GoalCategory.MIND,
                "Naming what you need can make conversations easier.")
        }
        when (dominantDifficult) {
            MoodType.TIRED, MoodType.SICK -> steps += SmallStep("Get to bed 30 minutes earlier", "😴", GoalCategory.SELF_CARE,
                "You felt ${dominantDifficult.label.lowercase()} more than anything else this week.")
            MoodType.ANXIOUS, MoodType.STRESSED, MoodType.OVERWHELMED -> steps += SmallStep("Do 3 minutes of box breathing", "🧘", GoalCategory.MIND,
                "Slow breathing can take the edge off feeling ${dominantDifficult.label.lowercase()}.")
            MoodType.SAD, MoodType.LONELY -> steps += SmallStep("Text a friend or family member", "💬", GoalCategory.SOCIAL,
                "A small connection can help when you feel ${dominantDifficult.label.lowercase()}.")
            MoodType.FRUSTRATED, MoodType.ANGRY -> steps += SmallStep("Take a 10-minute walk", "🚶", GoalCategory.BODY,
                "Moving your body can help release frustration.")
            else -> Unit
        }
        steps += SmallStep("Write down 3 things you're grateful for", "🙏", GoalCategory.MIND,
            "Noticing small good things builds on what's already working.")
        steps += SmallStep("Spend 15 minutes outside", "🌳", GoalCategory.BODY,
            "Fresh air and daylight are simple mood boosters.")
        return steps.distinctBy { it.title }
    }

    private fun questionFor(shape: Shape, heaviestTopic: String?, best: Pair<LocalDate, MoodType>?): String = when {
        heaviestTopic != null -> "What's one part of ${heaviestTopic.lowercase()} that's within your control next week?"
        shape == Shape.RISING -> "What helped the second half of your week feel better?"
        shape == Shape.FALLING -> "What changed midweek, and what would help next week start lighter?"
        best != null -> "What made ${dayName(best.first)} feel good, and how could you repeat it?"
        else -> "What's one thing you'd like more of next week?"
    }

    // ---------------------------------------------------------------------------------------------
    // Title
    // ---------------------------------------------------------------------------------------------

    private enum class Shape { RISING, FALLING, BRIGHT, HEAVY, MIXED, QUIET }

    private fun weekShape(weekMoods: List<MoodType?>): Shape {
        val logged = weekMoods.filterNotNull()
        if (logged.size < 2) return Shape.QUIET
        val firstHalf = weekMoods.take(4).filterNotNull()
        val secondHalf = weekMoods.drop(4).filterNotNull()
        val overall = positiveRate(logged)
        val difficult = logged.count { it.category == MoodCategory.DIFFICULT }.toDouble() / logged.size
        if (firstHalf.isNotEmpty() && secondHalf.isNotEmpty()) {
            val change = positiveRate(secondHalf) - positiveRate(firstHalf)
            if (change >= 0.4) return Shape.RISING
            if (change <= -0.4) return Shape.FALLING
        }
        return when {
            overall >= 0.7 -> Shape.BRIGHT
            difficult >= 0.6 -> Shape.HEAVY
            else -> Shape.MIXED
        }
    }

    private fun titleFor(shape: Shape, weekMoods: List<MoodType>): String = when (shape) {
        Shape.RISING -> "A slow start, a strong finish."
        Shape.FALLING -> "A strong start, a tougher finish."
        Shape.BRIGHT -> "A bright, steady week."
        Shape.HEAVY -> "A heavy week. You kept showing up."
        Shape.MIXED -> "A week of ups and downs."
        Shape.QUIET -> if (weekMoods.isEmpty()) "Your story starts here." else "A quiet week."
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** How often the day after a short night was difficult, compared with the day after a full night. */
    private data class SleepLink(val shortDifficultRate: Double, val restedDifficultRate: Double)

    private const val SHORT_SLEEP_HOURS = 6.0
    private const val RESTED_SLEEP_HOURS = 7.0

    /**
     * Looks for a link between sleep and the next day's mood over the lookback window.
     * Needs at least 2 short nights and 2 full nights with a mood logged the next day,
     * and short nights must be followed by difficult days noticeably more often. Otherwise returns null.
     */
    private fun sleepLink(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        sleepByDate: Map<LocalDate, Double>
    ): SleepLink? {
        val pairs = windowDates.mapNotNull { date ->
            val mood = moodByDate[date] ?: return@mapNotNull null
            val hours = sleepByDate[date] ?: return@mapNotNull null
            hours to mood
        }
        val short = pairs.filter { it.first < SHORT_SLEEP_HOURS }.map { it.second }
        val rested = pairs.filter { it.first >= RESTED_SLEEP_HOURS }.map { it.second }
        if (short.size < 2 || rested.size < 2) return null
        val shortRate = difficultRate(short)
        val restedRate = difficultRate(rested)
        return if (shortRate - restedRate >= 0.25) SleepLink(shortRate, restedRate) else null
    }

    private fun difficultRate(moods: List<MoodType>): Double =
        if (moods.isEmpty()) 0.0 else moods.count { it.category == MoodCategory.DIFFICULT }.toDouble() / moods.size

    private data class TopicLink(
        val topic: String,
        val positiveMentions: Int,
        val difficultMentions: Int,
        val difficultEntries: Int
    )

    /** For each topic: how often it's mentioned in entries on positive days vs difficult days. */
    private fun topicLinks(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        journalByDate: Map<LocalDate, String>
    ): List<TopicLink> {
        val entries = windowDates.mapNotNull { date ->
            val mood = moodByDate[date] ?: return@mapNotNull null
            val text = journalByDate[date] ?: return@mapNotNull null
            mood.category to words(text)
        }
        val difficultEntries = entries.count { it.first == MoodCategory.DIFFICULT }
        return TOPICS.map { (topic, keywords) ->
            TopicLink(
                topic = topic,
                positiveMentions = entries.count { it.first == MoodCategory.POSITIVE && it.second.any { w -> w in keywords } },
                difficultMentions = entries.count { it.first == MoodCategory.DIFFICULT && it.second.any { w -> w in keywords } },
                difficultEntries = difficultEntries
            )
        }
    }

    private fun bestDay(days: List<Pair<LocalDate, MoodType?>>): Pair<LocalDate, MoodType>? =
        days.lastOrNull { it.second?.category == MoodCategory.POSITIVE }?.let { it.first to it.second!! }

    private fun longestDifficultRun(days: List<Pair<LocalDate, MoodType?>>): Int {
        var best = 0
        var run = 0
        days.forEach { (_, mood) ->
            run = if (mood?.category == MoodCategory.DIFFICULT) run + 1 else 0
            if (run > best) best = run
        }
        return best
    }

    private fun positiveRate(moods: List<MoodType>): Double =
        if (moods.isEmpty()) 0.0 else moods.count { it.category == MoodCategory.POSITIVE }.toDouble() / moods.size

    private fun words(text: String): Set<String> =
        text.lowercase(Locale.ROOT).split(Regex("[^a-z']+")).filter { it.isNotBlank() }.toSet()

    private fun parse(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
    private fun pct(rate: Double) = "${(rate * 100).roundToInt()}%"
    private fun times(n: Int) = if (n == 1) "once" else "$n times"
    private fun dayName(date: LocalDate) = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    private fun plural(day: DayOfWeek) = day.getDisplayName(TextStyle.FULL, Locale.US) + "s"
}