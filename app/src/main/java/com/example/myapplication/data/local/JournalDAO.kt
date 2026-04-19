package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE) //replace if one exists
    suspend fun insertEntry(entry: JournalEntry)

    @Query("SELECT * From journal_entries WHERE date = :date")  //finds entry that matches passed date
    suspend fun getJournalByDate(date: String): JournalEntry?

    @Query("UPDATE journal_entries SET content = :content WHERE date = CURRENT_DATE")
    suspend fun updateEntry(content: String)

    @Query("UPDATE journal_entries SET complete = 1 WHERE date = CURRENT_DATE")
    suspend fun completeEntry()

    @Query("SELECT * FROM journal_entries") //pulls entire table
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("DELETE FROM journal_entries")   //deletes table
    suspend fun deleteAll()
}