package com.spatd.cleangallery

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(val id: Long, val uri: Uri, val type: MediaType) : Parcelable
