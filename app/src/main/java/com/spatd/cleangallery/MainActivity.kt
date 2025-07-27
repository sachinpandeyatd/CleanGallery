package com.spatd.cleangallery

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
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

class MainActivity : AppCompatActivity(), CardStackListener {

    private lateinit var cardStackView: CardStackView
    private lateinit var manager: CardStackLayoutManager
    private val adapter = CardStackAdapter()
    private val itemsToDelete = mutableListOf<MediaItem>()
    private lateinit var fabDelete: FloatingActionButton

//    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//        permissions ->
//            val isReadImageGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
//            val isReadVideoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
//            val isUserSelectedGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//                permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] ?: false
//            } else {
//                false
//            }
//            if (isReadImageGranted && isReadVideoGranted) {
//                Log.d("Permission", "Full media access granted")
//                fetchMedia()
//            }else if(isUserSelectedGranted){
//                Log.d("Permission", "Partial media access granted")
//                Toast.makeText(this, "Partial media access granted. You can select more photos later.", Toast.LENGTH_LONG).show()
//                fetchMedia()
//            }else{
//                Toast.makeText(this, "Storage permission is required to use this app", Toast.LENGTH_LONG).show()
//                finish()
//            }
//    }

    private val deleteRequestLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
        result ->
            if(result.resultCode == RESULT_OK){
                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Error deleting photo", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabDelete = findViewById(R.id.fab_delete)

        setupCardStackView()
        val folderName = intent.getStringExtra("FOLDER_NAME")

        if (folderName != null) {
            fetchMedia(folderName)
            title = folderName
        } else {
            Toast.makeText(this, "No folder specified.", Toast.LENGTH_LONG).show()
            finish()
        }

        fabDelete.setOnClickListener {
            deletePendingItems()
        }
    }

//    private fun checkPermissionsAndFetchMedia() {
//        val permissionsToRequest = when{
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->{
//                arrayOf(
//                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.READ_MEDIA_VIDEO,
//                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
//                )
//            }
//
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
//                arrayOf(
//                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.READ_MEDIA_VIDEO
//                )
//            }else ->{
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//            }
//        }
//
//        val allPermissionsGranted = permissionsToRequest.all {
//            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//        if(allPermissionsGranted){
//            fetchMedia()
//        }else{
//            requestPermissionLauncher.launch(permissionsToRequest)
//        }
//    }

    private fun setupCardStackView() {
        manager = CardStackLayoutManager(this, this).apply {
            setStackFrom(StackFrom.Top)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(20.0f)
            setDirections(Direction.HORIZONTAL)
            setCanScrollHorizontal(true)
            setCanScrollVertical(false)
            setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
            setOverlayInterpolator(LinearInterpolator())
        }

        cardStackView = findViewById(R.id.card_stack_view)
        cardStackView.layoutManager = manager
        cardStackView.adapter = adapter
        cardStackView.itemAnimator = DefaultItemAnimator()
    }

    private fun fetchMedia(folderName: String) {
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

            val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)" +
                    " AND ${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ?"

            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                folderName
            )

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

        when(direction){
            Direction.Left -> {
                Log.d("CardStackView", "Added to delete list: ${swipedItem.uri}")
                itemsToDelete.add(swipedItem)
                Toast.makeText(this, "Added to delete list", Toast.LENGTH_SHORT).show()
            }

            Direction.Right -> {
                Log.d("CardStackView", "Swiped Right. Keeping: ${swipedItem.uri}")
                Toast.makeText(this, "Kept", Toast.LENGTH_SHORT).show()
            }

            Direction.Top -> {
                Log.d("CardStackView", "Swiped Up. Favouriting: ${swipedItem.uri}")
                Toast.makeText(this, "Favourited", Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    private fun deleteMediaItems(items: List<MediaItem>) {
        if (items.isEmpty()) {
            Toast.makeText(this, "No photos selected for deletion.", Toast.LENGTH_SHORT).show()
            return
        }

        val urisToDelete = items.map { item ->
            when (item.type) {
                MediaType.VIDEO ->
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
                MediaType.IMAGE ->
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                deleteRequestLauncher.launch(intentSenderRequest)
                // After launching, clear the list
                itemsToDelete.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "Error requesting deletion: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Fallback for older devices (will delete one by one)
            var deleteCount = 0
            for (uri in urisToDelete) {
                try {
                    contentResolver.delete(uri, null, null)
                    deleteCount++
                } catch (ex: Exception) {
                    Log.e("DeleteFallback", "Failed to delete $uri", ex)
                }
            }
            Toast.makeText(this, "$deleteCount photos deleted", Toast.LENGTH_SHORT).show()
            itemsToDelete.clear()
        }
    }

    private fun deletePendingItems() {
        deleteMediaItems(itemsToDelete)
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}