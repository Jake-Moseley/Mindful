package com.example.myapplication.data.model

data class JournalEntry(
    val id: Int = 0,
    val date: String = "",
    val content: String = "",
    val complete: Boolean = false
)
