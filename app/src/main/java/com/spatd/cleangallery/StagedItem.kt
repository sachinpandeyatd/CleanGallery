package com.spatd.cleangallery
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staged_items")
data class StagedItem(
    @PrimaryKey val uri: String,
    val mediaId: Long,
    val mediaType: String,
    val status: String
)