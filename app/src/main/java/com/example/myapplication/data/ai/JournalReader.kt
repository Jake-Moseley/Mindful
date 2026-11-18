package com.example.myapplication.data.ai

import java.util.Locale

/** A feeling the user named in their own words, e.g. "stressed" or "proud". */
data class Feeling(val word: String, val positive: Boolean)

/** What one journal entry talks about. Everything here comes straight from the user's writing. */
data class EntryReading(
    val feelings: List<Feeling>,
    val people: List<String>,
    val activities: List<String>,
    val topics: List<String>,
    /** One short line from the entry worth reflecting back, or null if nothing fits. */
    val quote: String?,
    /**
     * True when the entry sounds really heavy (e.g. "I feel hopeless", "so empty").
     * The story never repeats these words; it shows a gentle support note instead.
     */
    val heavy: Boolean = false
) {
    val isEmpty: Boolean get() = feelings.isEmpty() && people.isEmpty() && activities.isEmpty() && topics.isEmpty()
    val positiveFeelings: List<Feeling> get() = feelings.filter { it.positive }
    val difficultFeelings: List<Feeling> get() = feelings.filter { !it.positive }
}

/**
 * Reads journal entries on the phone with simple word lists: no AI, nothing leaves the device.
 * Finds the feelings, people, activities and life topics an entry mentions, and picks a line to quote.
 */
object JournalReader {

    /** Words for feelings, mapped to the label shown to the user. */
    private val POSITIVE_FEELINGS = mapOf(
        "happy" to "happy", "glad" to "glad", "grateful" to "grateful", "thankful" to "grateful",
        "proud" to "proud", "excited" to "excited", "calm" to "calm", "relaxed" to "relaxed",
        "peaceful" to "peaceful", "hopeful" to "hopeful", "content" to "content", "loved" to "loved",
        "relieved" to "relieved", "confident" to "confident", "motivated" to "motivated",
        "energized" to "energized", "joy" to "joyful", "joyful" to "joyful", "accomplished" to "accomplished",
        "productive" to "productive", "rested" to "rested", "inspired" to "inspired", "appreciated" to "appreciated"
    )
    private val DIFFICULT_FEELINGS = mapOf(
        "sad" to "sad", "unhappy" to "sad", "anxious" to "anxious", "anxiety" to "anxious",
        "worried" to "worried", "worry" to "worried", "worrying" to "worried", "nervous" to "nervous",
        "stressed" to "stressed", "stress" to "stressed", "stressful" to "stressed",
        "overwhelmed" to "overwhelmed", "overwhelming" to "overwhelmed", "tired" to "tired",
        "exhausted" to "exhausted", "drained" to "drained", "lonely" to "lonely", "alone" to "lonely",
        "angry" to "angry", "mad" to "angry", "frustrated" to "frustrated", "frustrating" to "frustrated",
        "annoyed" to "annoyed", "upset" to "upset", "scared" to "scared", "afraid" to "scared",
        "hopeless" to "hopeless", "hurt" to "hurt", "sick" to "sick", "depressed" to "low",
        "miserable" to "miserable", "guilty" to "guilty", "ashamed" to "ashamed", "jealous" to "jealous",
        "bored" to "bored", "cried" to "teary", "crying" to "teary", "burnt" to "burnt out", "burned" to "burnt out",
        "worthless" to "worthless", "empty" to "empty", "numb" to "numb", "trapped" to "trapped",
        "helpless" to "helpless", "useless" to "useless"
    )

    /** Feelings so heavy the story responds with care instead of describing them. */
    val HEAVY_FEELINGS = setOf("hopeless", "worthless", "helpless", "empty", "numb", "trapped", "useless", "miserable", "low")

    /** Heavy words that only count when they describe a feeling ("I feel empty", not "the gym was empty"). */
    private val CONTEXT_WORDS = setOf("empty", "numb", "trapped", "useless", "low")
    private val FEELING_CUES = setOf("feel", "feels", "felt", "feeling", "i'm", "im", "am", "so", "just", "completely", "totally", "really", "been", "very")

    /** Words that flip a feeling ("not happy", "wasn't stressed"). */
    private val NEGATORS = setOf("not", "never", "no", "hardly", "barely", "without")

    private val PEOPLE = mapOf(
        "mom" to "your mom", "mother" to "your mom", "mum" to "your mom",
        "dad" to "your dad", "father" to "your dad", "parents" to "your parents",
        "sister" to "your sister", "brother" to "your brother", "siblings" to "your siblings",
        "friend" to "friends", "friends" to "friends", "bestie" to "friends",
        "boyfriend" to "your boyfriend", "girlfriend" to "your girlfriend", "partner" to "your partner",
        "husband" to "your husband", "wife" to "your wife",
        "roommate" to "your roommate", "roommates" to "your roommates",
        "coworker" to "coworkers", "coworkers" to "coworkers", "colleague" to "coworkers", "colleagues" to "coworkers",
        "boss" to "your boss", "manager" to "your boss",
        "professor" to "your professor", "teacher" to "your teacher", "classmates" to "classmates",
        "grandma" to "your grandma", "grandmother" to "your grandma", "grandpa" to "your grandpa", "grandfather" to "your grandpa",
        "son" to "your son", "daughter" to "your daughter", "kids" to "your kids",
        "family" to "family", "therapist" to "your therapist", "counselor" to "your counselor",
        "dog" to "your dog", "puppy" to "your dog", "cat" to "your cat", "kitten" to "your cat"
    )

    private val ACTIVITIES = mapOf(
        "walk" to "a walk", "walked" to "a walk", "walking" to "a walk",
        "run" to "a run", "ran" to "a run", "running" to "a run", "jog" to "a run", "jogging" to "a run",
        "gym" to "working out", "workout" to "working out", "exercise" to "working out", "exercised" to "working out", "lifting" to "working out",
        "yoga" to "yoga", "hike" to "a hike", "hiked" to "a hike", "hiking" to "a hike",
        "swim" to "swimming", "swam" to "swimming", "swimming" to "swimming",
        "reading" to "reading", "book" to "reading",
        "cook" to "cooking", "cooked" to "cooking", "cooking" to "cooking", "baked" to "baking", "baking" to "baking",
        "music" to "music", "guitar" to "music", "piano" to "music", "singing" to "music", "concert" to "music",
        "gaming" to "gaming", "videogames" to "gaming",
        "movie" to "a movie", "movies" to "a movie", "netflix" to "a show",
        "meditate" to "meditating", "meditated" to "meditating", "meditation" to "meditating",
        "church" to "church", "prayed" to "prayer", "pray" to "prayer",
        "coffee" to "coffee", "dinner" to "dinner", "lunch" to "lunch", "brunch" to "brunch",
        "party" to "a party", "shopping" to "shopping",
        "drawing" to "art", "painting" to "art", "painted" to "art",
        "nap" to "a nap", "napped" to "a nap", "beach" to "the beach", "park" to "the park",
        "outside" to "time outside", "outdoors" to "time outside",
        "studied" to "studying", "studying" to "studying", "cleaned" to "cleaning", "cleaning" to "cleaning"
    )

    /** Life topics, with the words that point to each one. */
    val TOPICS: Map<String, Set<String>> = mapOf(
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

    val TOPIC_EMOJI = mapOf(
        "Work" to "💼", "School" to "📚", "Sleep" to "😴", "Family" to "🏡", "Friends" to "🫂",
        "Exercise" to "🏃", "Money" to "💵", "Health" to "🩺", "Relationships" to "💞", "Nature" to "🌳"
    )

    private const val MAX_QUOTE_CHARS = 110

    fun read(text: String): EntryReading {
        val tokens = tokens(text)
        val feelings = feelingsIn(tokens)
        val heavy = feelings.any { it.word in HEAVY_FEELINGS } ||
                PatternAnalyzer.HIGH_RISK_PHRASES.any { text.lowercase(Locale.ROOT).contains(it) }
        val wordSet = tokens.toSet()
        return EntryReading(
            feelings = feelings.distinctBy { it.word },
            people = tokens.mapNotNull { PEOPLE[it] }.distinct(),
            activities = tokens.mapNotNull { ACTIVITIES[it] }.distinct(),
            topics = TOPICS.filter { (_, words) -> words.any { it in wordSet } }.keys.toList(),
            quote = pickQuote(text),
            heavy = heavy
        )
    }

    /** The feelings named in a list of words, skipping negated ones ("not happy"). */
    private fun feelingsIn(tokens: List<String>): List<Feeling> {
        val feelings = mutableListOf<Feeling>()
        tokens.forEachIndexed { i, word ->
            val positive = POSITIVE_FEELINGS[word]
            val difficult = DIFFICULT_FEELINGS[word]
            if ((positive != null || difficult != null) && !isNegated(tokens, i)) {
                // "empty", "numb" etc. only count when they describe how the user feels
                val needsCue = word in CONTEXT_WORDS
                if (!needsCue || (1..3).any { back -> tokens.getOrNull(i - back) in FEELING_CUES }) {
                    feelings += Feeling(positive ?: difficult!!, positive != null)
                }
            }
        }
        return feelings
    }

    /** True if one of the two words before position [i] negates it ("not happy", "wasn't really stressed"). */
    private fun isNegated(tokens: List<String>, i: Int): Boolean =
        (1..2).any { back ->
            val prev = tokens.getOrNull(i - back) ?: return@any false
            prev in NEGATORS || prev.endsWith("n't") || prev in NEGATED_CONTRACTIONS
        }

    /** Contractions typed without the apostrophe. */
    private val NEGATED_CONTRACTIONS = setOf("dont", "didnt", "wasnt", "isnt", "werent", "arent", "doesnt", "cant", "couldnt", "wont")

    /**
     * Picks the sentence that says the most (named feelings count most), shortened to fit on a card.
     * Never quotes a sentence with high-risk language back to the user.
     */
    private fun pickQuote(text: String): String? {
        val sentences = text.split(Regex("(?<=[.!?])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.split(" ").size >= 3 }
            .filter { s -> PatternAnalyzer.HIGH_RISK_PHRASES.none { s.lowercase(Locale.ROOT).contains(it) } }
            // Only quote lines without hard feelings in them: quotes are meant to be nice to see again
            .filter { s -> feelingsIn(tokens(s)).none { !it.positive } }
        if (sentences.isEmpty()) return null
        val best = sentences.maxByOrNull { s ->
            val words = tokens(s)
            feelingsIn(words).count { it.positive } * 3 +
                    words.count { it in PEOPLE || it in ACTIVITIES } +
                    TOPICS.values.count { topic -> words.any { it in topic } }
        } ?: return null
        return shorten(best)
    }

    private fun shorten(sentence: String): String {
        if (sentence.length <= MAX_QUOTE_CHARS) return sentence
        val cut = sentence.take(MAX_QUOTE_CHARS)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > 40) cut.take(lastSpace) else cut).trimEnd(',', ';', ':', ' ') + "…"
    }

    private fun tokens(text: String): List<String> =
        text.lowercase(Locale.ROOT).replace('’', '\'').split(Regex("[^a-z']+")).filter { it.isNotBlank() }
}