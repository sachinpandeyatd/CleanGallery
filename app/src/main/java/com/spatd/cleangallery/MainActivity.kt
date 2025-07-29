package com.spatd.cleangallery

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeableMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting

class MainActivity : AppCompatActivity(), CardStackListener {

    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager
    private val adapter = CardStackAdapter()
    private val itemsToDelete = mutableListOf<MediaItem>()
    private lateinit var fabDelete: FloatingActionButton
    private var currentToast: Toast? = null
    private val db by lazy { AppDatabase.getDatabase(this).stagedItemDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_layout)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            insets
        }

        fabDelete = findViewById(R.id.fab_delete)

        setupCardStackView()
        val folderName = intent.getStringExtra("FOLDER_NAME")

        if (folderName != null) {
            fetchMedia(folderName)
            title = folderName
        } else {
            fetchMedia(null)
            title = "All Photos"
        }

        fabDelete.setOnClickListener {
            lifecycleScope.launch {
                val trashItems = db.getItemsByStatus("TRASH")
                if (trashItems.isNotEmpty()) {
                    // Convert StagedItems to MediaItems
                    val trashMediaItems = trashItems.mapNotNull { stagedItem ->
                        try {
                            MediaItem(
                                id = stagedItem.mediaId,
                                uri = Uri.parse(stagedItem.uri),
                                type = MediaType.valueOf(stagedItem.mediaType)
                            )
                        } catch (e: Exception) { null }
                    }

                    // Launch ReviewActivity to manage the trash
                    val intent = Intent(this@MainActivity, ReviewActivity::class.java)
                    intent.putParcelableArrayListExtra("ITEMS_TO_REVIEW", ArrayList(trashMediaItems))
                    startActivity(intent) // We don't need a result back here

                } else {
                    Toast.makeText(this@MainActivity, "Trash is empty", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCardStackView() {
        manager = CardStackLayoutManager(this, this).apply {
            setStackFrom(StackFrom.Top)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(20.0f)
//            setDirections(Direction.FREEDOM)
            setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }

        cardStackView = findViewById(R.id.card_stack_view)
        cardStackView.layoutManager = manager
        cardStackView.adapter = adapter
        cardStackView.itemAnimator = DefaultItemAnimator()
    }

    private fun fetchMedia(folderName: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaList = mutableListOf<MediaItem>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

            val selection : String?
            val selectionArgs : Array<String>?

            if(folderName != null) {
                selection =
                    "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)" +
                            " AND ${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ?"

                selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                    folderName
                )
            }else{
                selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
                        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
                selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )
            }

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                    val type = when (cursor.getInt(mediaTypeColumn)) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaType.VIDEO
                        else -> MediaType.IMAGE
                    }
                    mediaList.add(MediaItem(id, contentUri, type))
                }
            }

            withContext(Dispatchers.Main) {
                adapter.setItems(mediaList)
            }
        }
    }

    override fun onCardSwiped(direction: Direction?) {
        val position = manager.topPosition - 1;
        val swipedItem = adapter.getItems().getOrNull(position) ?: return
        currentToast?.cancel()

        when(direction){
            Direction.Left -> {
                val itemToStage = StagedItem(
                    uri = swipedItem.uri.toString(),
                    mediaId = swipedItem.id,
                    mediaType = swipedItem.type.name,
                    status = "TRASH"
                )
                lifecycleScope.launch { db.insert(itemToStage) }
                currentToast = Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT)
            }

            Direction.Right -> {
                Log.d("CardStackView", "Swiped Right. Keeping: ${swipedItem.uri}")
                currentToast = Toast.makeText(this, "Kept", Toast.LENGTH_SHORT)
            }

            Direction.Top -> {
                val itemToStage = StagedItem(
                    uri = swipedItem.uri.toString(),
                    mediaId = swipedItem.id,
                    mediaType = swipedItem.type.name,
                    status = "FAVORITE"
                )
                lifecycleScope.launch { db.insert(itemToStage) }
                currentToast = Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT)
            }

            else -> {
                return
            }
        }
        currentToast?.show()

        if (manager.topPosition == adapter.itemCount) {
            Toast.makeText(this, "All done! 🎉", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && itemsToDelete.isNotEmpty()) {
            lifecycleScope.launch {
                val itemsToStage = itemsToDelete.map { mediaItem ->
                    StagedItem(
                        uri = mediaItem.uri.toString(),
                        mediaId = mediaItem.id,
                        mediaType = mediaItem.type.name,
                        status = "TRASH"
                    )
                }
                for (item in itemsToStage) {
                    db.insert(item)
                }
                itemsToDelete.clear()
                Log.d("MainActivity", "Session trash moved to persistent database.")
            }
        }
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}