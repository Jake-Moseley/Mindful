package com.example.myapplication.ui.mood

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.MoodDAO
import com.example.myapplication.data.model.MoodEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.sql.Date
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class MoodViewModel(private val moodDAO: MoodDAO) : ViewModel() {
    val moodEntries: StateFlow<List<MoodEntry>> = moodDAO.getAllMoods().stateIn( // Live mood entry updates
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    fun logMood(date: LocalDate?, mood: MoodType) {
        if (date == null) return
        viewModelScope.launch {
            moodDAO.insertMood(MoodEntry(date = date.toString(), mood = mood.ordinal))
        }
    }
    fun getMoodForDate(date: LocalDate): MoodType? {
        val entry = moodEntries.value.find { it.date == date.toString() }
        return entry?.let { MoodType.fromInt(it.mood) }
    }
}

class MoodViewModelFactory(
    private val moodDAO: MoodDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoodViewModel(moodDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}