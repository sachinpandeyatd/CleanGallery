package com.spatd.cleangallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReviewAdapter(
    private val items: MutableList<MediaItem>,
    private val onItemRemoved: (position: Int) -> Unit
) : RecyclerView.Adapter<ReviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.review_image_view)
        val removeIcon: ImageView = view.findViewById(R.id.remove_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.itemView.context).load(item.uri).into(holder.imageView)

        holder.removeIcon.setOnClickListener {
            // When the remove icon is clicked, notify the activity via the callback
            onItemRemoved(holder.adapterPosition)
        }
    }

    override fun getItemCount() = items.size
}