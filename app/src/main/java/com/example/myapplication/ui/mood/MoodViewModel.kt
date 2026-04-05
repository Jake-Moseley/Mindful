package com.example.myapplication.ui.mood

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class MoodViewModel : ViewModel() {
    var moodEntries by mutableStateOf(
        listOf(
            MoodEntry(LocalDate.now().minusDays(3), MoodType.TIRED)
        )
    )
        private set

    fun logMood(date: LocalDate?, mood: MoodType) {
        val updated = moodEntries.filterNot { it.date == date } + MoodEntry(date, mood)
        moodEntries = updated.sortedByDescending { it.date }
    }

    fun getMoodForDate(date: LocalDate): MoodType? {
        return moodEntries.find { it.date == date }?.mood
    }

}
