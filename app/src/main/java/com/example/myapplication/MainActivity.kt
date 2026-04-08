package com.example.forumactivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var postList = mutableListOf<ForumPost>()
    private lateinit var adapter: ForumAdapter
    private val CHANNEL_ID = "motivational_reminders"
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private val quotes = listOf(
        "Believe you can and you're halfway there.",
        "Your only limit is your mind.",
        "Don't stop until you're proud."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("forum_prefs", Context.MODE_PRIVATE)
        createNotificationChannel()
        loadPosts()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ForumAdapter(postList) { position ->
            val post = postList[position]
            if (post.isLiked) {
                post.likes -= 1
                post.isLiked = false
            } else {
                post.likes += 1
                post.isLiked = true
            }
            savePosts()
            adapter.notifyItemChanged(position)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnPost = findViewById<Button>(R.id.btnPost)

        btnPost.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotEmpty()) {
                val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                // Create post with name (Make sure your name is used here!)
                val newPost = ForumPost("Reeja", text, currentTime, 0, false)
                postList.add(newPost)
                savePosts()
                adapter.notifyItemInserted(postList.size - 1)
                edtMessage.text.clear()
                recyclerView.scrollToPosition(postList.size - 1)
            }
        }

        sendMotivationalReminder()
    }

    private fun savePosts() {
        val json = gson.toJson(postList)
        sharedPreferences.edit().putString("saved_posts", json).apply()
    }

    private fun loadPosts() {
        val json = sharedPreferences.getString("saved_posts", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ForumPost>>() {}.type
            postList = gson.fromJson(json, type)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Motivational Reminders"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendMotivationalReminder() {
        val quote = quotes[Random.nextInt(quotes.size)]
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Inspiration")
            .setContentText(quote)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(100, builder.build())
    }
}
