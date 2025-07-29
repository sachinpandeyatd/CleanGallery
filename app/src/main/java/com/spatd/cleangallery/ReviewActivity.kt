package com.spatd.cleangallery

import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ReviewActivity : AppCompatActivity() {

    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var deleteFinalButton: Button
    private lateinit var adapter: ReviewAdapter
    private lateinit var mediaItemsToReview: MutableList<MediaItem>
    private val db by lazy { AppDatabase.getDatabase(this).stagedItemDao() }
    private var itemsCurrentlyBeingDeleted: List<MediaItem>? = null


    private val deleteRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Media deleted successfully", Toast.LENGTH_SHORT).show()

                itemsCurrentlyBeingDeleted?.let { items ->
                    lifecycleScope.launch {
                        val urisToRemove = items.map { it.uri.toString() }
                        db.deleteItemsByUri(urisToRemove)
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Deletion was cancelled", Toast.LENGTH_SHORT).show()
            }
            itemsCurrentlyBeingDeleted = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        mediaItemsToReview = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("ITEMS_TO_REVIEW", MediaItem::class.java)
        } else {
            intent.getParcelableArrayListExtra("ITEMS_TO_REVIEW")
        } ?: mutableListOf()

        reviewRecyclerView = findViewById(R.id.review_recycler_view)
        deleteFinalButton = findViewById(R.id.delete_final_button)

        setupToolbar()
        setupRecyclerView()

        deleteFinalButton.setOnClickListener {
            if (mediaItemsToReview.isNotEmpty()) {
                deleteMediaItems(mediaItemsToReview)
            } else {
                finish()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = ReviewAdapter(mediaItemsToReview) { position ->
            val itemToRestore = mediaItemsToReview[position]

            lifecycleScope.launch {
                db.deleteItemsByUri(listOf(itemToRestore.uri.toString()))
                Log.d("ReviewActivity", "Item removed from trash list: ${itemToRestore.uri}")
            }

            mediaItemsToReview.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, mediaItemsToReview.size)
        }
        reviewRecyclerView.adapter = adapter
    }

    private fun deleteMediaItems(items: List<MediaItem>) {
        itemsCurrentlyBeingDeleted = items

        val urisToDelete = items.map { item ->
            when (item.type) {
                MediaType.VIDEO -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
                MediaType.IMAGE -> ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                deleteRequestLauncher.launch(intentSenderRequest)
            } catch (e: Exception) {
                Log.e("DeleteError", "Error creating delete request", e)
                Toast.makeText(this, "Error requesting deletion: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                itemsCurrentlyBeingDeleted = null // Clear if it fails
            }
        } else {
            // Fallback for older devices
            var deleteCount = 0
            for (uri in urisToDelete) {
                try {
                    contentResolver.delete(uri, null, null)
                    deleteCount++
                } catch (ex: Exception) {
                    Log.e("DeleteFallback", "Failed to delete $uri", ex)
                }
            }
            Toast.makeText(this, "$deleteCount media files deleted", Toast.LENGTH_SHORT).show()
            // In the fallback, also delete from our DB and finish
            lifecycleScope.launch {
                val urisToRemove = items.map { it.uri.toString() }
                db.deleteItemsByUri(urisToRemove)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}