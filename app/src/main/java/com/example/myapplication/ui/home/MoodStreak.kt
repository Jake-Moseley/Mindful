package com.example.myapplication.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/**
 * Counts how many days in a row the user has logged a mood.
 *
 * The streak counts back from today. If today hasn't been logged yet it counts
 * back from yesterday instead, so the streak isn't shown as broken until the
 * day has actually been missed.
 *
 * @param loggedDates dates in ISO format (yyyy-MM-dd), as stored in MoodEntry.date
 */
@RequiresApi(Build.VERSION_CODES.O)
fun calculateMoodStreak(loggedDates: Collection<String>, today: LocalDate = LocalDate.now()): Int {
    val dates = loggedDates.toHashSet()
    var day = if (today.toString() in dates) today else today.minusDays(1)
    var streak = 0
    while (day.toString() in dates) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}
