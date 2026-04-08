package com.example.myapplication.ui.forum

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

data class ForumPost(
    val author: String,
    val message: String,
    val timestamp: String,
    var likes: Int = 0,
    var isLiked: Boolean = false
)

class ForumAdapter(
    private val postList: List<ForumPost>,
    private val onLikeClicked: (Int) -> Unit
) : RecyclerView.Adapter<ForumAdapter.ForumViewHolder>() {

    class ForumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardAvatar: CardView = itemView.findViewById(R.id.cardAvatar)
        val txtAvatarLetter: TextView = itemView.findViewById(R.id.txtAvatarLetter)
        val authorText: TextView = itemView.findViewById(R.id.txtAuthor)
        val messageText: TextView = itemView.findViewById(R.id.txtMessage)
        val timeText: TextView = itemView.findViewById(R.id.txtTimestamp)
        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val txtLikeCount: TextView = itemView.findViewById(R.id.txtLikeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return ForumViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForumViewHolder, position: Int) {
        val currentPost = postList[position]
        holder.authorText.text = currentPost.author
        holder.messageText.text = currentPost.message
        holder.timeText.text = currentPost.timestamp
        holder.txtLikeCount.text = currentPost.likes.toString()

        if (currentPost.author.isNotEmpty()) {
            holder.txtAvatarLetter.text = currentPost.author.take(1).uppercase()
            val colors = listOf("#FF7043", "#42A5F5", "#66BB6A", "#FFA726", "#AB47BC")
            val colorIndex = currentPost.author.length % colors.size
            holder.cardAvatar.setCardBackgroundColor(Color.parseColor(colors[colorIndex]))
        }

        holder.btnLike.setColorFilter(if (currentPost.isLiked) Color.RED else Color.GRAY)
        holder.btnLike.setOnClickListener { onLikeClicked(position) }
    }

    override fun getItemCount(): Int = postList.size
}