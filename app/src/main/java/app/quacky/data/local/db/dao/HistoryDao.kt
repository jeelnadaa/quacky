package app.quacky.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.quacky.data.local.db.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history_entry ORDER BY createdAt DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_entry WHERE toolId = :toolId ORDER BY createdAt DESC")
    fun getHistoryForToolFlow(toolId: String): Flow<List<HistoryEntryEntity>>

    @Query("SELECT * FROM history_entry WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritesFlow(): Flow<List<HistoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntryEntity): Long

    @Query("SELECT * FROM history_entry WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntryEntity?

    @Query("UPDATE history_entry SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM history_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history_entry WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM history_entry WHERE toolId = :toolId")
    suspend fun clearHistoryForTool(toolId: String)

    @Query("DELETE FROM history_entry")
    suspend fun clearAll()

    @Query("DELETE FROM history_entry WHERE createdAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("""
        DELETE FROM history_entry 
        WHERE toolId = :toolId 
        AND id NOT IN (
            SELECT id FROM history_entry 
            WHERE toolId = :toolId 
            ORDER BY createdAt DESC 
            LIMIT :maxEntries
        )
    """)
    suspend fun trimMaxEntriesForTool(toolId: String, maxEntries: Int)
}
