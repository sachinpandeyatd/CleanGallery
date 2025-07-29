package com.spatd.cleangallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class FolderAdapter(private var folders: List<MediaFolder>) :
    RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.folder_thumbnail)
        val name: TextView = view.findViewById(R.id.folder_name)
        val count: TextView = view.findViewById(R.id.folder_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.name.text = folder.name
        holder.count.text = "${folder.count} photos"

        Glide.with(holder.itemView.context)
            .load(folder.thumbnailUri)
            .placeholder(R.color.material_grey_300)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MainActivity::class.java).apply {
                // Pass the folder name to MainActivity
                putExtra("FOLDER_NAME", folder.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = folders.size
}