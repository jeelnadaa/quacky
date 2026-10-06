package app.quacky.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "saved_wheel")
data class SavedWheelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wheel_option",
    foreignKeys = [
        ForeignKey(
            entity = SavedWheelEntity::class,
            parentColumns = ["id"],
            childColumns = ["wheelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wheelId")]
)
data class WheelOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wheelId: Long,
    val label: String,
    val weight: Int = 1
)

@Entity(tableName = "saved_group")
data class SavedGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_member",
    foreignKeys = [
        ForeignKey(
            entity = SavedGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val skillRating: Int = 3
)

@Entity(tableName = "saved_countdown")
data class SavedCountdownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_template")
data class SavedTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_usage")
data class ToolUsageEntity(
    @PrimaryKey
    val toolId: String,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val count: Int = 1
)
