package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.ResourceEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourcesDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE) //replace if one exists
    suspend fun insertResource(entry: ResourceEntry)

    @Query("SELECT * FROM resources WHERE name = :name")  //finds resource that matches passed date
    suspend fun getResourceByName(name: String): ResourceEntry?

    @Query("SELECT * FROM resources") //pulls entire table
    fun getAllResources(): Flow<List<ResourceEntry>>

    @Query("DELETE FROM resources")   //deletes table
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM resources")
    suspend fun count(): Int

    /*
    @Query("UPDATE resources SET description = :text WHERE date = CURRENT_DATE")
    suspend fun updateResource(text: String)
     */
}