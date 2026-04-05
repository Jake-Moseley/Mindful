package com.example.forumactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ForumAdapter(private val postList: List<ForumPost>) :
    RecyclerView.Adapter<ForumAdapter.ForumViewHolder>() {

    // This class finds the specific text views in your item_post.xml
    class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorText: TextView = itemView.findViewById(R.id.txtAuthor)
        val messageText: TextView = itemView.findViewById(R.id.txtMessage)
    }

    // This "inflates" the item_post layout for each row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ForumViewHolder(view)
    }

    // This puts the actual data into the text views
    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        val currentPost = postList[position]
        holder.authorText.text = currentPost.author
        holder.messageText.text = currentPost.message
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}