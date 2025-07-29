package com.spatd.cleangallery

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this).stagedItemDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        setupToolbar()
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view)
        fetchAndDisplayFavorites()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun fetchAndDisplayFavorites() {
        lifecycleScope.launch {
            val favoriteStagedItems = db.getItemsByStatus("FAVORITE")
            val favoriteMediaItems : List<MediaItem> = favoriteStagedItems.map {stagedItem ->
                try {
                    MediaItem(id = -1, uri = Uri.parse(stagedItem.uri), type = MediaType.IMAGE)
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()

            val mutableList = favoriteMediaItems.toMutableList()

            val adapter = ReviewAdapter(mutableList) { position ->
                val itemToRemove = mutableList[position]
                lifecycleScope.launch {
                    db.deleteItemsByUri(listOf(itemToRemove.uri.toString()))
                }
                mutableList.removeAt(position)
                favoritesRecyclerView.adapter?.notifyItemRemoved(position)
                favoritesRecyclerView.adapter?.notifyItemRangeChanged(position, favoriteMediaItems.size)
            }
            favoritesRecyclerView.adapter = adapter
        }
    }
}