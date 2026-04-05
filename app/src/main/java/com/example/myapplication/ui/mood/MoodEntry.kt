package com.example.myapplication.ui.mood

import java.time.LocalDate

data class MoodEntry(
    val date: LocalDate?,
    val mood: MoodType
)