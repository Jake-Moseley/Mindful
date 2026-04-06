package com.example.myapplication.data.model

import android.R

data class JournalEntry(
    val id: Int = 0,
    val date: String = "",
    val content: String = "",
    val complete: Boolean = false
)
