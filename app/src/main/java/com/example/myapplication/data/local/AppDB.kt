package com.example.myapplication.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.model.GoalCompletion
import com.example.myapplication.data.model.GoalEntity
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.data.model.ResourceEntry

@Database(
    entities = [
        MoodEntry::class,
        JournalEntry::class,
        ResourceEntry::class,
        GoalEntity::class,
        GoalCompletion::class
    ],
    version = 4, // change when adding on different features i.e. journalEntry (4 = goals)
    exportSchema = false
)
abstract class AppDB : RoomDatabase() {
    abstract fun moodDAO(): MoodDAO
    abstract fun journalDAO(): JournalDAO

    abstract fun resourceDAO(): ResourcesDAO

    abstract fun goalDAO(): GoalDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null

        /**
         * Version 3 -> 4: adds the goals and goal_completions tables.
         * Existing moods, journal entries and resources are kept.
         * When the database changes again, add a new Migration (4 -> 5) instead of editing this one.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdDate TEXT NOT NULL,
                        isArchived INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS goal_completions (
                        goalId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(goalId, date)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    "mindful_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}