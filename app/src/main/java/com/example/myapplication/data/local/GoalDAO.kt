package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.model.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDAO {

    // ---- Goals ----

    @Query("SELECT * FROM goals WHERE isArchived = 0 ORDER BY id")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    /** All goals ever created, archived ones included. Used to decide whether to add the starter goals. */
    @Query("SELECT COUNT(*) FROM goals")
    suspend fun countAllGoals(): Int

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Query("UPDATE goals SET title = :title, emoji = :emoji, category = :category WHERE id = :id")
    suspend fun updateGoal(id: Int, title: String, emoji: String, category: String)

    @Query("UPDATE goals SET isArchived = 1 WHERE id = :id")
    suspend fun archiveGoal(id: Int)

    // ---- Completions ----

    @Query("SELECT * FROM goal_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<GoalCompletion>>

    /** Every day on which at least one goal was completed, newest first. Used for the streak. */
    @Query("SELECT DISTINCT date FROM goal_completions ORDER BY date DESC")
    fun getCompletionDates(): Flow<List<String>>

    /** Every completion ever recorded. Used by the Weekly Story to connect goals and mood. */
    @Query("SELECT * FROM goal_completions")
    fun getAllCompletions(): Flow<List<GoalCompletion>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: GoalCompletion)

    /** Testing tools only: clears all goal history. */
    @Query("DELETE FROM goal_completions")
    suspend fun deleteAllCompletions()

    @Query("DELETE FROM goal_completions WHERE goalId = :goalId AND date = :date")
    suspend fun deleteCompletion(goalId: Int, date: String)
}