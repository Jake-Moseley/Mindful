package com.example.myapplication.data.model
import androidx.room.PrimaryKey
import androidx.room.Entity

@Entity(tableName = "resources")
data class ResourceEntry(
    @PrimaryKey(autoGenerate = true)        //should auto generate ids
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val phoneNum: String = ""
)
