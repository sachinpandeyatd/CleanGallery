package com.spatd.cleangallery

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class ReviewActivity : AppCompatActivity() {

    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var deleteFinalButton: Button
    private lateinit var adapter: ReviewAdapter
    private lateinit var mediaItemsToReview: MutableList<MediaItem>

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
            val resultIntent = Intent()
            resultIntent.putParcelableArrayListExtra("FINAL_DELETE_LIST", ArrayList(mediaItemsToReview))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
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
            mediaItemsToReview.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, mediaItemsToReview.size)
        }
        reviewRecyclerView.adapter = adapter
    }
}