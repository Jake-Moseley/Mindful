package com.example.myapplication.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.model.JournalEntry
import com.example.myapplication.data.model.MoodEntry
import com.example.myapplication.data.model.ResourceEntry

@Database(
    entities = [
        MoodEntry::class,
        JournalEntry::class,
        ResourceEntry::class
    ],
    version = 3, // change when adding on different features i.e. journalEntry
    exportSchema = false
)
abstract class AppDB : RoomDatabase() {
    abstract fun moodDAO(): MoodDAO
    abstract fun journalDAO(): JournalDAO

    abstract fun resourceDAO(): ResourcesDAO
    companion object {
        @Volatile
        private var INSTANCE: AppDB? = null
        fun getDatabase(context: Context): AppDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDB::class.java,
                    "mindful_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}