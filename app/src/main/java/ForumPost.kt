package com.example.forumactivity
data class ForumPost(
    val author: String = "Anonymous",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)