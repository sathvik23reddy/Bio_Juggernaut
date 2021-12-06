package com.example.bio_juggernaut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bio_juggernaut.models.Post
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class PostAdapter(options: FirestoreRecyclerOptions<Post>, private val listener: IPostAdapter) : FirestoreRecyclerAdapter<Post, PostAdapter.PostViewHolder>(
    options
) {
    class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val postText: TextView = itemView.findViewById(R.id.postTitle)
        val userText: TextView = itemView.findViewById(R.id.userName)
        val createdAt: TextView = itemView.findViewById(R.id.createdAt)
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val reportImage: ImageView = itemView.findViewById(R.id.reportImg)
        val address: TextView = itemView.findViewById(R.id.addressTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val viewHolder =  PostViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.rv_item, parent, false))
        viewHolder.itemView.setOnClickListener{
            listener.onPostClicked(snapshots.getSnapshot(viewHolder.adapterPosition).id)
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, model: Post) {
        holder.postText.text = model.text
        holder.userText.text = model.createdBy.displayName
        holder.address.text = model.address
        holder.createdAt.text = Utils.getTimeAgo(model.createdAt)
        Glide.with(holder.reportImage.context).load(model.imgUri).centerCrop().into(holder.reportImage)
        Glide.with(holder.userImage.context).load(model.createdBy.imageUrl).circleCrop().into(holder.userImage)
    }
}

interface IPostAdapter {
    fun onPostClicked(postID: String)

}

