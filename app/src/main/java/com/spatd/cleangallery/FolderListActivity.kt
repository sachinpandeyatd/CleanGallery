package com.spatd.cleangallery

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FolderListActivity : AppCompatActivity() {

    private lateinit var folderRecyclerView: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView

    @SuppressLint("NewApi")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isReadImagesGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            val isReadVideoGranted = permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
            val isUserSelectedGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] ?: false
            } else {
                false
            }

            if ((isReadImagesGranted && isReadVideoGranted) || isUserSelectedGranted) {
                // Permissions granted (either full or partial)
                Log.d("Permissions", "Media access granted.")
                fetchMediaFolders()
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to use this app", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_folder_list)


        val mainContent = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content)
        val topAppBarLayout = findViewById<AppBarLayout>(R.id.top_app_bar_layout)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        folderRecyclerView = findViewById(R.id.folder_recycler_view)

        setSupportActionBar(toolbar)
        folderRecyclerView.layoutManager = LinearLayoutManager(this)

//        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_folders
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_photos -> {
                    Toast.makeText(this, "Photos clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_folders -> {
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainContent) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            topAppBarLayout.updatePadding(top = systemBars.top)
            bottomNavigationView.updatePadding(bottom = systemBars.bottom)

//            view.updatePadding(top = systemBars.top)
//
//            folderRecyclerView.updatePadding(bottom = systemBars.bottom)

            insets
        }

        checkPermissionsAndFetchFolders()
    }

    private fun checkPermissionsAndFetchFolders() {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
            else -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            fetchMediaFolders()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun fetchMediaFolders() {
        lifecycleScope.launch(Dispatchers.IO) {
            val folderMap = mutableMapOf<String, MediaFolder>()

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val projection = arrayOf(
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" +
                    " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            val sortOrder = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"

            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

                val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val bucketName = cursor.getString(bucketNameColumn)

                    if (bucketName == null) {
                        Log.w("FetchFolders", "Skipping a media file with a null bucket name.")
                        continue
                    }

                    val id = cursor.getLong(idColumn)
                    val mediaType = cursor.getInt(mediaTypeColumn)
                    val thumbnailUri = ContentUris.withAppendedId(collection, id)

                    val folder = folderMap[bucketName]
                    if (folder == null) {
                        folderMap[bucketName] = MediaFolder(bucketName, thumbnailUri, 1)
                    } else {
                        folderMap[bucketName] = folder.copy(count = folder.count + 1)
                    }
                }
            }

            val folderList = folderMap.values.toList().sortedBy { it.name }

            withContext(Dispatchers.Main) {
                folderRecyclerView.adapter = FolderAdapter(folderList)
            }
        }
    }
}