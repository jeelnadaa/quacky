package app.quacky.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_entry",
    indices = [
        Index("toolId"),
        Index("createdAt"),
        Index("isFavorite")
    ]
)
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolId: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val payloadJson: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
