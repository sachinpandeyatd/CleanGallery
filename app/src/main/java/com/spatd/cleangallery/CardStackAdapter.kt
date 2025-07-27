package com.spatd.cleangallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CardStackAdapter(private var items: List<MediaItem> = emptyList()) :
    RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_media_card, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItems(): List<MediaItem> {
        return items
    }

    fun setItems(newItems: List<MediaItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.media_image_view)
        private val playIcon: ImageView = view.findViewById(R.id.play_icon)

        fun bind(item: MediaItem) {
            Glide.with(itemView.context)
                .load(item.uri)
                .into(imageView)

            if (item.type == MediaType.VIDEO) {
                playIcon.visibility = View.VISIBLE

                itemView.setOnClickListener {
                    val context = itemView.context
                    val playIntent = Intent(Intent.ACTION_VIEW, item.uri).apply {
                        setDataAndType(item.uri, "video/*")
                    }
                    context.startActivity(playIntent)
                }
            } else {
                playIcon.visibility = View.GONE
                itemView.setOnClickListener(null)
            }
        }
    }
}