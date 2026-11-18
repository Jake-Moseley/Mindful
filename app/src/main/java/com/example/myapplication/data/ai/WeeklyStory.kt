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

/** A fun stat tile on the "At a glance" page, e.g. "Mood of the week · 😊 Happy". */
data class Highlight(val emoji: String, val value: String, val label: String)

/** One day of the week with a mood, a journal entry, or both, and what the entry talked about. */
data class DayMoment(
    val date: LocalDate,
    val mood: MoodType?,
    val hasEntry: Boolean,
    val quote: String?,
    val feelings: List<Feeling>,
    val people: List<String>,
    val activities: List<String>,
    val topics: List<String>,
    /** The entry sounded really heavy. Its words are never repeated back. */
    val heavy: Boolean = false,
    /** A kind line for the day card on harder days, shown instead of a quote. */
    val gentleNote: String? = null
) {
    val positiveFeelings: List<Feeling> get() = feelings.filter { it.positive }
    val difficultFeelings: List<Feeling> get() = feelings.filter { !it.positive }

    /** Short labels for the day card: feelings first, then people, activities and topics. */
    val tags: List<String>
        get() {
            // Family, Friends, Exercise and Nature are already covered by people and activities
            val shownTopics = topics.filter { it in STANDALONE || (people.isEmpty() && activities.isEmpty()) }
            // Only good feelings are shown back; hard ones are never listed
            return (positiveFeelings.map { it.word } + people + activities + shownTopics.map { it.lowercase() }).distinct()
        }

    companion object {
        val STANDALONE = setOf("Work", "School", "Money", "Health", "Sleep", "Relationships")
    }
}

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
    /** A detailed summary of the week that ties moods to what the user wrote. */
    val recap: String,
    /** Each day with a mood or an entry, oldest first. Used for the recap and Gemini's prompt. */
    val moments: List<DayMoment>,
    /** 2-4 stat tiles for the "At a glance" page. */
    val highlights: List<Highlight>,
    /** Some of this week's writing sounded really heavy: the story shows a support note instead of details. */
    val heavyWriting: Boolean,
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

    private const val LOOKBACK_DAYS = 30L

    /** Topics that say something on their own. Family, Friends, Exercise and Nature are covered by people and activities. */
    private val STANDALONE_TOPICS = DayMoment.STANDALONE

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

        // Read this week's journal entries
        val moments = weekDates.mapNotNull { date ->
            val mood = moodByDate[date]
            val text = journalByDate[date]
            if (mood == null && text == null) return@mapNotNull null
            val reading = text?.let { JournalReader.read(it) }
            val draft = DayMoment(
                date = date,
                mood = mood,
                hasEntry = text != null,
                quote = reading?.quote,
                feelings = reading?.feelings.orEmpty(),
                people = reading?.people.orEmpty(),
                activities = reading?.activities.orEmpty(),
                topics = reading?.topics.orEmpty(),
                heavy = reading?.heavy == true
            )
            // Hard days are never quoted back. The card gets a kind line instead.
            if (draft.heavy || tone(draft) == MoodCategory.DIFFICULT) {
                draft.copy(
                    quote = null,
                    gentleNote = if (draft.hasEntry) "A heavier day · you still made space to write" else "A heavier day · you still checked in"
                )
            } else draft
        }
        val heavyWriting = moments.any { it.heavy }

        val split = MoodCategory.entries.associateWith { cat -> weekMoods.count { it.category == cat } }
        val topMoods = weekMoods.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(3).map { it.key to it.value }

        // Window for patterns that need more than a week of data
        val windowStart = weekEnd.minusDays(LOOKBACK_DAYS - 1)
        val windowDates = generateSequence(windowStart) { it.plusDays(1) }.takeWhile { !it.isAfter(weekEnd) }.toList()

        val shape = weekShape(weekMoods = days.map { it.second })
        val hasData = weekMoods.isNotEmpty() || journalsThisWeek > 0

        val topicLinks = topicLinks(windowDates, moodByDate, journalByDate)
        val sleepLink = sleepLink(windowDates, moodByDate, sleepByDate)
        val felt = buildFelt(weekMoods, prevWeekMoods, split, weekDates.size, days, moments, heavyWriting)
        val lifted = buildLifted(windowDates, moodByDate, goalsByDate, topicLinks, days, moments)
        val weighed = buildWeighed(windowDates, moodByDate, topicLinks, days, sleepLink, moments, heavyWriting)

        val dominantDifficult = weekMoods.filter { it.category == MoodCategory.DIFFICULT }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val heaviestTopic = topicLinks.filter { it.difficultMentions > it.positiveMentions }
            .maxByOrNull { it.difficultMentions }?.topic
            ?: moments.filter { tone(it) == MoodCategory.DIFFICULT }.flatMap { it.topics }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return WeeklyStory(
            weekStart = weekStart,
            weekEnd = weekEnd,
            isCurrentWeek = !weekEnd.isBefore(today),
            hasData = hasData,
            title = if (heavyWriting) "A week to be gentle with yourself." else titleFor(shape, weekMoods, moments),
            days = days,
            daysLogged = weekMoods.size,
            goalsCompleted = goalsThisWeek,
            journalEntries = journalsThisWeek,
            categorySplit = split,
            topMoods = topMoods,
            recap = buildRecap(shape, weekMoods, moments, heavyWriting),
            moments = moments,
            highlights = buildHighlights(weekMoods, days, moments, weekDates.sumOf { d -> journalByDate[d]?.let { wordCount(it) } ?: 0 }),
            heavyWriting = heavyWriting,
            felt = felt,
            lifted = lifted,
            weighed = weighed,
            smallSteps = smallStepsFor(dominantDifficult, heaviestTopic, sleepLink != null, moments),
            reflectionQuestion = if (heavyWriting) "What's one kind thing you could do for yourself this week?"
            else questionFor(shape, heaviestTopic, bestDay(days), moments)
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The recap: a detailed summary in the user's own words
    // ---------------------------------------------------------------------------------------------

    /**
     * Two short sentences: the shape of the week, then one gentle "what asked a lot / what lifted you" contrast.
     * No counts (they're on the cover) and no quotes. Hard words from the journal are never repeated.
     */
    private fun buildRecap(shape: Shape, weekMoods: List<MoodType>, moments: List<DayMoment>, heavyWriting: Boolean): String {
        if (moments.isEmpty()) {
            return "Nothing has been logged for this week yet. Log a mood or write a few lines, and your story will start to take shape here."
        }
        val sentences = mutableListOf<String>()

        // 1. The shape of the week
        when (shape) {
            Shape.RISING -> sentences += "Your week started slow and finished strong."
            Shape.FALLING -> sentences += "Your week started strong and asked more of you toward the end."
            Shape.BRIGHT -> sentences += "This was a bright, steady week."
            Shape.HEAVY -> sentences += "This week asked a lot of you, and you kept showing up."
            Shape.MIXED -> sentences += "Your week had its ups and downs."
            Shape.QUIET -> Unit // too little data for a shape; the contrast below says more
        }

        // 2. One contrast: what asked a lot of you, and what lifted you
        val bright = moments.lastOrNull { tone(it) == MoodCategory.POSITIVE && upliftingIn(it).isNotEmpty() }
            ?.let { m -> "${dayName(m.date)} with ${listPhrase(upliftingIn(m).take(2))} was a bright spot" }
        val hard = hardThing(moments)?.let { gentleTopic(it) }
            ?: if (moments.any { tone(it) == MoodCategory.DIFFICULT } && shape != Shape.HEAVY) "a few days asked a lot of you" else null
        when {
            heavyWriting -> sentences += "Some of what you wrote sounded really heavy, so please be gentle with yourself."
            hard != null && bright != null -> sentences += "${capitalize(hard)}, but $bright."
            hard != null -> sentences += "${capitalize(hard)}, and you still showed up for yourself."
            bright != null -> sentences += "$bright."
        }

        // With very little logged, add a friendly nudge
        if (sentences.size < 2) {
            sentences += when {
                moments.none { it.hasEntry } -> "Writing a few lines next to your mood helps your story notice more."
                weekMoods.isEmpty() -> "Logging a mood next to your writing helps your story see the bigger picture."
                else -> "You took a moment to check in with yourself, and that counts."
            }
        }
        return sentences.take(2).joinToString(" ")
    }

    /** Fun stat tiles: mood of the week, brightest day, what was on your mind, words written. */
    private fun buildHighlights(
        weekMoods: List<MoodType>,
        days: List<Pair<LocalDate, MoodType?>>,
        moments: List<DayMoment>,
        wordsWritten: Int
    ): List<Highlight> {
        val tiles = mutableListOf<Highlight>()
        val topGood = weekMoods.filter { it.category == MoodCategory.POSITIVE }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        when {
            topGood != null -> tiles += Highlight(topGood.emoji, topGood.label, "Mood of the week")
            weekMoods.isNotEmpty() -> tiles += Highlight("🫶", "${weekMoods.size} ${if (weekMoods.size == 1) "check-in" else "check-ins"}", "You kept showing up")
        }
        // Brightest day: the good day with the most going on (latest wins a tie)
        moments.filter { it.mood?.category == MoodCategory.POSITIVE }
            .maxWithOrNull(compareBy<DayMoment>({ upliftingIn(it).size + it.positiveFeelings.size }, { it.date }))
            ?.let { m -> tiles += Highlight(m.mood!!.emoji, dayName(m.date), "Brightest day") }
        moments.flatMap { m -> m.topics.filter { it in STANDALONE_TOPICS || it in setOf("Family", "Friends", "Exercise", "Nature") } }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?.let { topic -> tiles += Highlight(JournalReader.TOPIC_EMOJI[topic] ?: "💭", topic, "On your mind") }
        if (tiles.size < 4) {
            moments.filter { tone(it) == MoodCategory.POSITIVE }.flatMap { upliftingIn(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                ?.let { thing -> tiles += Highlight(boostFor(thing).emoji, capitalize(thing), "Your booster") }
        }
        if (wordsWritten > 0 && tiles.size < 4) tiles += Highlight("✍️", "$wordsWritten", if (wordsWritten == 1) "word written" else "words written")
        return tiles.take(4)
    }

    /** The standalone topic that showed up most on harder days this week, if any. */
    private fun hardThing(moments: List<DayMoment>): String? =
        moments.filter { tone(it) == MoodCategory.DIFFICULT }
            .flatMap { m -> m.topics.filter { it in STANDALONE_TOPICS } }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

    /** A kind way to say a topic was hard. */
    private fun gentleTopic(topic: String): String = when (topic) {
        "Work" -> "work seemed to take a lot out of you"
        "School" -> "school seemed to take a lot out of you"
        "Money" -> "money worries took up a lot of space"
        "Health" -> "your health asked a lot of you"
        "Sleep" -> "rest was hard to come by"
        "Relationships" -> "relationships took a lot of energy"
        else -> "${topic.lowercase()} asked a lot of you"
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 1: How you felt
    // ---------------------------------------------------------------------------------------------

    private fun buildFelt(
        weekMoods: List<MoodType>,
        prevWeekMoods: List<MoodType>,
        split: Map<MoodCategory, Int>,
        weekLength: Int,
        days: List<Pair<LocalDate, MoodType?>>,
        moments: List<DayMoment>,
        heavyWriting: Boolean
    ): StoryChapter {
        if (weekMoods.isEmpty() && moments.none { it.hasEntry }) {
            return StoryChapter("No moods were logged this week. Logging once a day helps your story come together.", emptyList())
        }
        val evidence = mutableListOf<Evidence>()
        val logged = days.filter { it.second != null }.map { it.first to it.second!! }
        val positive = split[MoodCategory.POSITIVE] ?: 0
        val difficult = split[MoodCategory.DIFFICULT] ?: 0

        // The headline: one sentence about the overall feel
        val headline = when {
            weekMoods.isEmpty() -> "No moods logged, but you made time to write, and that says a lot."
            weekMoods.size == 1 -> {
                val (date, mood) = logged.first()
                if (mood.category == MoodCategory.DIFFICULT) "${dayName(date)} ${gentleFor(mood).pred}, and you still checked in."
                else "You checked in feeling ${mood.label.lowercase()} on ${dayName(date)}."
            }
            positive > weekMoods.size / 2 -> "Most of your days felt good. 🌞"
            difficult > weekMoods.size / 2 -> "This week asked a lot of you, and you kept checking in."
            positive > 0 -> "A mix of good days and harder ones, which is a very normal week."
            else -> "A quieter week, mood-wise."
        }

        // Chips: things the headline doesn't already say
        val goodWords = moments.flatMap { it.positiveFeelings }.groupingBy { it.word }.eachCount().entries
            .sortedByDescending { it.value }.map { it.key }.take(3)
        if (goodWords.isNotEmpty()) evidence += Evidence("💬", "Good feelings you named: ${goodWords.joinToString(", ")}")
        if (prevWeekMoods.size >= 2 && weekMoods.size >= 2) {
            val diff = positiveRate(weekMoods) - positiveRate(prevWeekMoods)
            when {
                diff >= 0.2 -> evidence += Evidence("📈", "Brighter than the week before")
                diff <= -0.2 -> evidence += Evidence("🫶", "A bit harder than last week, so go easy on yourself")
            }
        }
        moments.firstOrNull { it.mood?.category == MoodCategory.DIFFICULT && it.positiveFeelings.isNotEmpty() }?.let { m ->
            evidence += Evidence("🌱", "Even on ${dayName(m.date)}, you found room to feel ${m.positiveFeelings.first().word}")
        }
        if (weekMoods.size in 2..3) {
            evidence += Evidence("📅", logged.joinToString(" · ") { (date, mood) ->
                "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} ${mood.emoji}"
            })
        }

        return StoryChapter(headline, evidence.take(3))
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 2: What lifted you
    // ---------------------------------------------------------------------------------------------

    /** A theme for something that lifted the user, e.g. "a walk" → "Moving your body". */
    private data class Boost(val theme: String, val emoji: String)

    private fun boostFor(thing: String): Boost = when (thing) {
        "your dog", "your cat" -> Boost("Time with your pet", "🐾")
        "a walk", "a run", "working out", "yoga", "swimming" -> Boost("Moving your body", "🏃")
        "a hike", "the park", "the beach", "time outside" -> Boost("Getting outside", "🌳")
        "meditating", "church", "prayer", "a nap" -> Boost("Slowing down", "🧘")
        "reading", "music", "art", "cooking", "baking", "a movie", "gaming", "coffee",
        "dinner", "lunch", "brunch", "a party", "shopping" -> Boost("Doing things you enjoy", "🎨")
        else -> Boost("Time with people you care about", "🫂")
    }

    private fun buildLifted(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        goalsByDate: Map<LocalDate, Int>,
        topics: List<TopicLink>,
        days: List<Pair<LocalDate, MoodType?>>,
        moments: List<DayMoment>
    ): StoryChapter {
        val evidence = mutableListOf<Evidence>()
        val themes = mutableListOf<String>()

        // Goals and mood: are goal days happier days?
        val logged = windowDates.filter { it in moodByDate }
        val goalDays = logged.filter { (goalsByDate[it] ?: 0) > 0 }
        val otherDays = logged.filter { (goalsByDate[it] ?: 0) == 0 }
        if (goalDays.size >= 3 && otherDays.size >= 3) {
            val withGoals = positiveRate(goalDays.map { moodByDate.getValue(it) })
            val without = positiveRate(otherDays.map { moodByDate.getValue(it) })
            if (withGoals - without >= 0.2) {
                themes += "Finishing your goals"
                evidence += Evidence("🎯", "Goal days were good days ${pct(withGoals)} of the time, vs ${pct(without)} otherwise")
            }
        }

        // What showed up on good days, grouped into themes
        val goodDays = moments.filter { tone(it) == MoodCategory.POSITIVE }
        val boosts = goodDays.flatMap { m -> upliftingIn(m).map { thing -> Triple(boostFor(thing), thing, m.date) } }
        boosts.groupBy { it.first }.entries
            .sortedByDescending { it.value.size }
            .take(2)
            .forEach { (boost, items) ->
                themes += boost.theme
                val things = items.map { it.second }.distinct().take(2)
                val dayNames = items.map { it.third }.distinct().map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US) }
                evidence += Evidence(boost.emoji, "${capitalize(listPhrase(things))} · ${dayNames.joinToString(", ")}")
            }

        // Topics that keep showing up on good days (last 30 days)
        topics.filter { it.positiveMentions >= 2 && it.positiveMentions > it.difficultMentions }
            .maxByOrNull { it.positiveMentions }
            ?.let { link ->
                evidence += Evidence(JournalReader.TOPIC_EMOJI[link.topic] ?: "✨", "${link.topic} came up on ${link.positiveMentions} good days this month")
            }

        // One line in their own words from a good day
        goodDays.lastOrNull { it.quote != null }?.let { m -> evidence += Evidence("💬", "“${m.quote}”") }

        val headline = when {
            themes.size >= 2 -> "${themes[0]} and ${themes[1].replaceFirstChar { it.lowercase() }} seem to give you a boost."
            themes.size == 1 -> "${themes[0]} seems to give you a boost."
            moments.any { it.positiveFeelings.isNotEmpty() } -> {
                val m = moments.first { it.positiveFeelings.isNotEmpty() }
                "You found a moment to feel ${m.positiveFeelings.first().word} on ${dayName(m.date)}. Hold on to that."
            }
            bestDay(days) != null -> "${dayName(bestDay(days)!!.first)} was a bright spot. Writing about good days helps your story learn what lifts you."
            else -> "Bright spots will show up here as you log more moods, goals and journal entries."
        }
        return StoryChapter(headline, evidence.take(3))
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 3: What asked a lot of you
    // ---------------------------------------------------------------------------------------------

    /**
     * "What asked a lot of you." Every hard thing is said kindly and paired with something the user did.
     * Hard words and quotes from hard days are never repeated. Very heavy writing gets a support note instead.
     */
    private fun buildWeighed(
        windowDates: List<LocalDate>,
        moodByDate: Map<LocalDate, MoodType>,
        topics: List<TopicLink>,
        days: List<Pair<LocalDate, MoodType?>>,
        sleep: SleepLink?,
        moments: List<DayMoment>,
        heavyWriting: Boolean
    ): StoryChapter {
        if (heavyWriting) {
            return StoryChapter(
                "Some of what you wrote this week sounded really heavy. You don't have to carry that alone. " +
                        "Talking with someone you trust, or a professional, can really help.",
                listOf(Evidence("🫶", "You kept writing through a hard week. That takes strength."))
            )
        }
        val evidence = mutableListOf<Evidence>()
        val headlines = mutableListOf<String>()

        // Short nights and the day after (from Health Connect)
        if (sleep != null) {
            evidence += Evidence("😴", "Harder days followed ${pct(sleep.shortDifficultRate)} of nights under ${SHORT_SLEEP_HOURS.toInt()}h")
            headlines += "Short nights seem to make the next day harder, so rest is worth protecting."
        }

        // The topic that showed up most on harder days: this month, or this week
        val monthTopic = topics.filter { it.difficultMentions >= 2 && it.difficultMentions > it.positiveMentions }
            .maxByOrNull { it.difficultMentions }
        val topic = monthTopic?.topic ?: hardThing(moments)
        if (topic != null) {
            headlines += "${topic} came up most on your harder days, so ${tipFor(topic)}."
            if (monthTopic != null) {
                evidence += Evidence(JournalReader.TOPIC_EMOJI[topic] ?: "☁️", "$topic came up on ${monthTopic.difficultMentions} harder days this month")
            }
        }

        // Weekday pattern over the lookback window
        windowDates.filter { it in moodByDate }
            .groupBy { it.dayOfWeek }
            .mapNotNull { (day, dates) ->
                val difficult = dates.map { moodByDate.getValue(it) }.filter { it.category == MoodCategory.DIFFICULT }
                if (difficult.size >= 2 && difficult.size.toDouble() / dates.size >= 0.66) Triple(day, difficult.size, dates.size) else null
            }
            .maxByOrNull { it.second }
            ?.let { (day, difficult, total) ->
                evidence += Evidence("📅", "$difficult of your last $total ${plural(day)} felt harder")
                headlines += "${plural(day)} seem to ask more of you, so plan something kind for them."
            }

        // Harder days in a row this week
        val streak = longestDifficultRun(days)
        if (streak >= 3) evidence += Evidence("💪", "You kept going through $streak harder days in a row")

        val hardDays = moments.filter { tone(it) == MoodCategory.DIFFICULT }
        val headline = headlines.firstOrNull()
            ?: when {
                hardDays.size == 1 -> "${dayName(hardDays.first().date)} ${gentleFor(hardDays.first()).pred}, and you still showed up for yourself."
                hardDays.size > 1 -> "A few days felt like a lot to carry, and you kept showing up."
                else -> "Nothing stood out as asking too much of you this week. 🌤️"
            }
        return StoryChapter(headline, evidence.take(3))
    }

    /** A small, kind tip for a topic that showed up on harder days. */
    private fun tipFor(topic: String): String = when (topic) {
        "Work", "School" -> "small breaks could go a long way"
        "Money" -> "a small plan might take some weight off"
        "Health" -> "be extra gentle with yourself"
        "Sleep" -> "a calmer evening could help"
        "Relationships", "Family", "Friends" -> "naming what you need could help"
        else -> "a little extra care could help"
    }

    /** Kind ways to say a day was hard, without repeating the user's own hard words. */
    private enum class Gentle(val pred: String) {
        HEAVY("felt really heavy"),
        PRESSURE("felt like a lot to carry"),
        ENERGY("left you running low on energy"),
        TENDER("felt a bit tender"),
        FRUSTRATION("got frustrating"),
        FLAT("felt a bit flat"),
        HARD("felt harder")
    }

    private fun gentleWord(word: String): Gentle = when (word) {
        in JournalReader.HEAVY_FEELINGS, "guilty", "ashamed" -> Gentle.HEAVY
        "anxious", "worried", "nervous", "stressed", "overwhelmed", "scared" -> Gentle.PRESSURE
        "tired", "exhausted", "drained", "burnt out", "sick" -> Gentle.ENERGY
        "sad", "lonely", "hurt", "teary", "jealous" -> Gentle.TENDER
        "angry", "frustrated", "annoyed", "upset" -> Gentle.FRUSTRATION
        "bored", "meh", "confused" -> Gentle.FLAT
        else -> Gentle.HARD
    }

    private fun gentleFor(mood: MoodType): Gentle = gentleWord(mood.label.lowercase())

    private fun gentleForFeelings(feelings: List<Feeling>): Gentle =
        feelings.filter { !it.positive }.map { gentleWord(it.word) }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: Gentle.HARD

    /** The gentle phrase for a day: heavy writing first, then the logged mood, then the feelings in the entry. */
    private fun gentleFor(m: DayMoment): Gentle = when {
        m.heavy -> Gentle.HEAVY
        m.mood != null && m.mood.category == MoodCategory.DIFFICULT -> gentleFor(m.mood)
        else -> gentleForFeelings(m.feelings)
    }

    // ---------------------------------------------------------------------------------------------
    // Chapter 4: Looking ahead
    // ---------------------------------------------------------------------------------------------

    /** Small steps built from things that showed up on good days. */
    private val ACTIVITY_STEPS = mapOf(
        "a walk" to Triple("Take a 15-minute walk", "🚶", GoalCategory.BODY),
        "a run" to Triple("Go for a short run", "🏃", GoalCategory.BODY),
        "working out" to Triple("Do a 20-minute workout", "💪", GoalCategory.BODY),
        "yoga" to Triple("Do 10 minutes of yoga", "🧘", GoalCategory.BODY),
        "a hike" to Triple("Spend 15 minutes outside", "🌳", GoalCategory.BODY),
        "the park" to Triple("Spend 15 minutes outside", "🌳", GoalCategory.BODY),
        "the beach" to Triple("Spend 15 minutes outside", "🌳", GoalCategory.BODY),
        "time outside" to Triple("Spend 15 minutes outside", "🌳", GoalCategory.BODY),
        "reading" to Triple("Read for 15 minutes", "📖", GoalCategory.MIND),
        "meditating" to Triple("Meditate for 5 minutes", "🧘", GoalCategory.MIND),
        "cooking" to Triple("Cook something you enjoy", "🍳", GoalCategory.SELF_CARE),
        "baking" to Triple("Bake something you enjoy", "🧁", GoalCategory.SELF_CARE),
        "music" to Triple("Listen to a favorite album", "🎧", GoalCategory.SELF_CARE),
        "art" to Triple("Spend 15 minutes making art", "🎨", GoalCategory.SELF_CARE)
    )
    private val NOT_SOCIAL_STEPS = setOf("coworkers", "your boss", "your professor", "your teacher", "classmates")

    private fun smallStepsFor(
        dominantDifficult: MoodType?,
        heaviestTopic: String?,
        shortSleepHurts: Boolean,
        moments: List<DayMoment>
    ): List<SmallStep> {
        val steps = mutableListOf<SmallStep>()

        // Do more of what showed up on good days
        val good = moments.filter { tone(it) == MoodCategory.POSITIVE }
        good.flatMap { it.activities }.firstOrNull { it in ACTIVITY_STEPS }?.let { activity ->
            val (title, emoji, category) = ACTIVITY_STEPS.getValue(activity)
            steps += SmallStep(title, emoji, category, "${capitalize(activity)} showed up on a good day this week.")
        }
        good.flatMap { it.people }.firstOrNull { it !in NOT_SOCIAL_STEPS }?.let { person ->
            val title = when {
                person == "friends" -> "Text a friend"
                person == "your dog" || person == "your cat" -> "Spend time with $person"
                else -> "Reach out to $person"
            }
            steps += SmallStep(title, "💬", GoalCategory.SOCIAL, "${capitalize(person)} came up on a good day this week.")
        }

        if (shortSleepHurts) {
            steps += SmallStep("Get to bed 30 minutes earlier", "😴", GoalCategory.SELF_CARE,
                "Your sleep data shows harder days after short nights.")
        }
        when (heaviestTopic) {
            "Work", "School" -> steps += SmallStep("Take a 10-minute break outside", "🌳", GoalCategory.BODY,
                "A short break away from ${heaviestTopic.lowercase()} can reset a heavy day.")
            "Sleep" -> steps += SmallStep("No screens 1 hour before bed", "📵", GoalCategory.SELF_CARE,
                "A calmer evening can make rest come easier.")
            "Money" -> steps += SmallStep("Spend 10 minutes planning your budget", "💵", GoalCategory.SELF_CARE,
                "A small plan can make money worries feel more manageable.")
            "Family", "Relationships", "Friends" -> steps += SmallStep("Write down one thing you need", "📝", GoalCategory.MIND,
                "Naming what you need can make conversations easier.")
        }
        when (dominantDifficult) {
            MoodType.TIRED, MoodType.SICK -> steps += SmallStep("Get to bed 30 minutes earlier", "😴", GoalCategory.SELF_CARE,
                "You've been running low on energy, so a little more rest could help.")
            MoodType.ANXIOUS, MoodType.STRESSED, MoodType.OVERWHELMED -> steps += SmallStep("Do 3 minutes of box breathing", "🧘", GoalCategory.MIND,
                "Slow breathing can help when things feel like a lot.")
            MoodType.SAD, MoodType.LONELY -> steps += SmallStep("Text a friend or family member", "💬", GoalCategory.SOCIAL,
                "A small connection can make tender days feel lighter.")
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

    private fun questionFor(
        shape: Shape,
        heaviestTopic: String?,
        best: Pair<LocalDate, MoodType>?,
        moments: List<DayMoment>
    ): String {
        val goodThing = moments.lastOrNull { tone(it) == MoodCategory.POSITIVE && upliftingIn(it).isNotEmpty() }
        return when {
            heaviestTopic != null -> "What's one part of ${heaviestTopic.lowercase()} that's within your control next week?"
            shape == Shape.RISING -> "What helped the second half of your week feel better?"
            shape == Shape.FALLING -> "What changed midweek, and what would help next week start lighter?"
            goodThing != null -> "What about ${upliftingIn(goodThing).first()} on ${dayName(goodThing.date)} felt good, and how could you make room for it again?"
            best != null -> "What made ${dayName(best.first)} feel good, and how could you repeat it?"
            moments.any { it.difficultFeelings.isNotEmpty() || it.heavy } -> "What's one kind thing you could do for yourself next week?"
            else -> "What's one thing you'd like more of next week?"
        }
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

    private fun titleFor(shape: Shape, weekMoods: List<MoodType>, moments: List<DayMoment>): String = when (shape) {
        Shape.RISING -> "A slow start, a strong finish."
        Shape.FALLING -> "A strong start, a tougher finish."
        Shape.BRIGHT -> "A bright, steady week."
        Shape.HEAVY -> "A harder week. You kept showing up."
        Shape.MIXED -> "A week of ups and downs."
        Shape.QUIET -> {
            val heavyTopic = moments.filter { tone(it) != MoodCategory.POSITIVE }
                .flatMap { m -> m.topics.filter { it in STANDALONE_TOPICS } }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            val goodThing = moments.filter { tone(it) == MoodCategory.POSITIVE }.flatMap { upliftingIn(it) }.firstOrNull()
            when {
                moments.isEmpty() -> "Your story starts here."
                heavyTopic != null -> "A week with ${heavyTopic.lowercase()} on your mind."
                goodThing != null -> "A week with room for $goodThing."
                weekMoods.size == 1 -> {
                    val word = weekMoods.first().label.lowercase()
                    "${if (word.first() in "aeiou") "An" else "A"} $word start."
                }
                else -> "A quiet week."
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Whether a day felt good or hard: the logged mood if there is one, otherwise the feelings in the entry. */
    private fun tone(m: DayMoment): MoodCategory? {
        m.mood?.let { return it.category }
        val good = m.positiveFeelings.size
        val hard = m.difficultFeelings.size
        return when {
            good > hard -> MoodCategory.POSITIVE
            hard > good -> MoodCategory.DIFFICULT
            else -> null
        }
    }

    /** What an entry talked about, as phrases that fit in a sentence: "work", "your mom", "a walk". */
    private fun thingsIn(m: DayMoment): List<String> {
        val topics = m.topics.filter { it in STANDALONE_TOPICS || (m.people.isEmpty() && m.activities.isEmpty()) }
        return (topics.map { it.lowercase() } + m.people + m.activities).distinct()
    }

    /** People and activities from a good day: the things that might have lifted the user (not bosses or deadlines). */
    private fun upliftingIn(m: DayMoment): List<String> =
        (m.people.filter { it !in NOT_SOCIAL_STEPS } + m.activities).distinct()

    /** Good feelings worth mentioning for a day, minus the logged mood itself. Hard feelings are never repeated. */
    private fun momentFeelings(m: DayMoment): List<String> =
        m.positiveFeelings.map { it.word }
            .filter { word -> m.mood == null || word != m.mood.label.lowercase() }
            .take(2)

    /**
     * One sentence about one day, e.g. "On Tuesday, when you felt anxious, you wrote about work and your mom."
     * Works with whatever the day has: a mood, things mentioned, feelings named.
     */
    private fun describeMoment(m: DayMoment, about: List<String>): String {
        val day = dayName(m.date)
        val things = about.take(3)
        val feelings = momentFeelings(m)
        val moodPart = m.mood?.let { ", when you felt ${it.label.lowercase()}," } ?: ","
        return when {
            things.isNotEmpty() && feelings.isNotEmpty() ->
                "On $day$moodPart you wrote about ${listPhrase(things)} and described feeling ${listPhrase(feelings)}."
            things.isNotEmpty() -> "On $day$moodPart you wrote about ${listPhrase(things)}."
            feelings.isNotEmpty() -> "On $day$moodPart you wrote about feeling ${listPhrase(feelings)}."
            m.mood != null -> "On $day you felt ${m.mood.label.lowercase()}."
            else -> "You wrote in your journal on $day."
        }
    }

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
        return JournalReader.TOPICS.map { (topic, keywords) ->
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

    /** "a", "a and b", "a, b and c" */
    private fun listPhrase(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
    }

    private fun wordCount(text: String) = text.split(Regex("\\s+")).count { it.any(Char::isLetterOrDigit) }
    private fun capitalize(text: String) = text.replaceFirstChar { it.uppercase() }
    private fun parse(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
    private fun pct(rate: Double) = "${(rate * 100).roundToInt()}%"
    private fun times(n: Int) = when (n) {
        1 -> "once"
        2 -> "twice"
        else -> "$n times"
    }
    private fun dayName(date: LocalDate) = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    private fun plural(day: DayOfWeek) = day.getDisplayName(TextStyle.FULL, Locale.US) + "s"
}