package app.quacky.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_palette")
data class SavedPaletteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "palette_color",
    foreignKeys = [
        ForeignKey(
            entity = SavedPaletteEntity::class,
            parentColumns = ["id"],
            childColumns = ["paletteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("paletteId")]
)
data class PaletteColorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paletteId: Long,
    val hex: String,
    val colorName: String
)
