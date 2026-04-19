package com.example.myapplication.data.model
import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey
    val date: String = "",
    val mood: Int = 0
)
