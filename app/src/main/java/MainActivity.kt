package com.example.forumactivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val posts = mutableListOf<ForumPost>()
    private lateinit var adapter: ForumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Forum RecyclerView and Adapter
        val recyclerView = findViewById<RecyclerView>(R.id.forumRecyclerView)
        adapter = ForumAdapter(posts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 2. Setup the "Post" button and Input field
        val postButton = findViewById<Button>(R.id.btnPost)
        val inputField = findViewById<EditText>(R.id.editPostInput)

        postButton.setOnClickListener {
            val message = inputField.text.toString().trim()
            if (message.isNotEmpty()) {
                // Add new post to our list
                posts.add(0, ForumPost(author = "Student", message = message))
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
                inputField.text.clear()
            } else {
                Toast.makeText(this, "Type something first!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Setup Notifications
        createNotificationChannel()
        val notifButton = findViewById<Button>(R.id.notificationButton)
        notifButton.setOnClickListener {
            sendNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MINDFUL_NOTIF",
                "Mindful Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Motivational daily reminders"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "MINDFUL_NOTIF")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mindful Reminder")
            .setContentText("Time to check in with the community!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(1, builder.build())
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Enable notifications in settings", Toast.LENGTH_SHORT).show()
        }
    }
}