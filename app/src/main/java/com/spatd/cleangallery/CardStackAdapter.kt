package com.spatd.cleangallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CardStackAdapter(private var items: List<MediaItem> = emptyList()) :
    RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.media_image_view)

        fun bind(item: MediaItem) {
            Glide.with(itemView.context).load(item.uri).into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.item_media_card, parent, false))
    }

    override fun onBindViewHolder(holder: CardStackAdapter.ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size;
    }

    fun getItems(): List<MediaItem>{
        return items;
    }

    fun setItems(items: List<MediaItem>){
        this.items = items;
        notifyDataSetChanged();
    }
}