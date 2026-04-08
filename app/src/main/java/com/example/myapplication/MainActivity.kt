package com.example.myapplication
import com.example.myapplication.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.ui.forum.ForumAdapter
import com.example.myapplication.ui.forum.ForumPost
import java.util.*


class MainActivity : AppCompatActivity() {
    private var postList = mutableListOf<ForumPost>()
    private lateinit var adapter: ForumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        MindfulApp()

        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = ForumAdapter(postList) { position ->
            val post = postList[position]
            post.isLiked = !post.isLiked
            post.likes += if (post.isLiked) 1 else -1
            adapter.notifyItemChanged(position)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun MindfulApp() {

    }
}
