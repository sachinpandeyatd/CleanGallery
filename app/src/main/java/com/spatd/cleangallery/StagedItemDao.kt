package com.spatd.cleangallery
import androidx.room.*

@Dao
interface StagedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StagedItem)

    @Query("SELECT * FROM staged_items WHERE status = :status")
    suspend fun getItemsByStatus(status: String): List<StagedItem>

    @Query("DELETE FROM staged_items WHERE uri IN (:uris)")
    suspend fun deleteItemsByUri(uris: List<String>)
}