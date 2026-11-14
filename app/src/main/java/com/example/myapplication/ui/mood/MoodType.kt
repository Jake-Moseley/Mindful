package com.example.myapplication.ui.mood

import androidx.compose.ui.graphics.Color

/** Groups moods so the calendar, the mood picker and Pattern Insights can treat similar moods alike. */
enum class MoodCategory(val label: String, val color: Color, val accent: Color) {
    POSITIVE("Positive", Color(0xFFD7F2DF), Color(0xFF1E6B3A)),
    NEUTRAL("Neutral", Color(0xFFDDF4F8), Color(0xFF1F5F6B)),
    DIFFICULT("Difficult", Color(0xFFFBE6CD), Color(0xFF8A4B00))
}

/**
 * Every mood a user can log.
 *
 * IMPORTANT: moods are saved in the database by their position in this list (ordinal).
 * Only ever ADD new moods to the END of the list. Never reorder or delete entries,
 * or moods that users already logged will change meaning.
 */
enum class MoodType(val label: String, val emoji: String, val category: MoodCategory) {
    // Original moods (positions 0-3), keep these first
    HAPPY("Happy", "😊", MoodCategory.POSITIVE),
    SAD("Sad", "😢", MoodCategory.DIFFICULT),
    FRUSTRATED("Frustrated", "😠", MoodCategory.DIFFICULT),
    TIRED("Tired", "🥱", MoodCategory.DIFFICULT),

    // Added in Version 2.0
    EXCITED("Excited", "🤩", MoodCategory.POSITIVE),
    GRATEFUL("Grateful", "🙏", MoodCategory.POSITIVE),
    CALM("Calm", "😌", MoodCategory.POSITIVE),
    CONFIDENT("Confident", "😎", MoodCategory.POSITIVE),
    LOVED("Loved", "🥰", MoodCategory.POSITIVE),
    PROUD("Proud", "😄", MoodCategory.POSITIVE),
    HOPEFUL("Hopeful", "🌱", MoodCategory.POSITIVE),
    MOTIVATED("Motivated", "💪", MoodCategory.POSITIVE),

    OKAY("Okay", "🙂", MoodCategory.NEUTRAL),
    MEH("Meh", "😐", MoodCategory.NEUTRAL),
    BORED("Bored", "😑", MoodCategory.NEUTRAL),
    CONFUSED("Confused", "😕", MoodCategory.NEUTRAL),
    SURPRISED("Surprised", "😮", MoodCategory.NEUTRAL),

    ANXIOUS("Anxious", "😟", MoodCategory.DIFFICULT),
    STRESSED("Stressed", "😫", MoodCategory.DIFFICULT),
    OVERWHELMED("Overwhelmed", "🤯", MoodCategory.DIFFICULT),
    ANGRY("Angry", "😡", MoodCategory.DIFFICULT),
    LONELY("Lonely", "😔", MoodCategory.DIFFICULT),
    SICK("Sick", "🤒", MoodCategory.DIFFICULT);

    companion object {
        fun fromInt(value: Int): MoodType {
            return entries.getOrElse(value) { HAPPY }
        }

        fun inCategory(category: MoodCategory): List<MoodType> = entries.filter { it.category == category }
    }
}