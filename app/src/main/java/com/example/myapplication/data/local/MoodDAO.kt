package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) //replace if one exists
    suspend fun insertMood(entry: MoodEntry)

    @Query("SELECT * From mood_entries WHERE date = :date")
    suspend fun getMoodByDate(date: String): MoodEntry?

    @Query("SELECT * FROM mood_entries")
    fun getAllMoods(): Flow<List<MoodEntry>>

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAll()
}