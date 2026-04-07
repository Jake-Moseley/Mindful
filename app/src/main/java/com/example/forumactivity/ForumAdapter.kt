package com.example.forumactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ForumAdapter(private val postList: List<ForumPost>) :
    RecyclerView.Adapter<ForumAdapter.ForumViewHolder>() {

    // Finds the views in your item_post.xml
    class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorText: TextView = itemView.findViewById(R.id.txtAuthor)
        val messageText: TextView = itemView.findViewById(R.id.txtMessage)
        val timestampText: TextView = itemView.findViewById(R.id.textViewTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ForumViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        val currentPost = postList[position]

        // Setting the basic text
        holder.authorText.text = currentPost.author
        holder.messageText.text = currentPost.message

        // Formatting the Long timestamp into a readable String
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = Date(currentPost.timestamp)
        holder.timestampText.text = sdf.format(date)
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}