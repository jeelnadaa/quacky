package app.quacky.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.quacky.data.local.db.entity.GroupMemberEntity
import app.quacky.data.local.db.entity.PaletteColorEntity
import app.quacky.data.local.db.entity.SavedCountdownEntity
import app.quacky.data.local.db.entity.SavedGroupEntity
import app.quacky.data.local.db.entity.SavedPaletteEntity
import app.quacky.data.local.db.entity.SavedTemplateEntity
import app.quacky.data.local.db.entity.SavedWheelEntity
import app.quacky.data.local.db.entity.SearchHistoryEntity
import app.quacky.data.local.db.entity.ToolUsageEntity
import app.quacky.data.local.db.entity.WheelOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaletteDao {
    @Query("SELECT * FROM saved_palette ORDER BY createdAt DESC")
    fun getAllPalettesFlow(): Flow<List<SavedPaletteEntity>>

    @Query("SELECT * FROM palette_color WHERE paletteId = :paletteId")
    suspend fun getColorsForPalette(paletteId: Long): List<PaletteColorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPalette(palette: SavedPaletteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColors(colors: List<PaletteColorEntity>)

    @Query("DELETE FROM saved_palette WHERE id = :id")
    suspend fun deletePalette(id: Long)
}

@Dao
interface WheelDao {
    @Query("SELECT * FROM saved_wheel ORDER BY createdAt DESC")
    fun getAllWheelsFlow(): Flow<List<SavedWheelEntity>>

    @Query("SELECT * FROM wheel_option WHERE wheelId = :wheelId")
    suspend fun getOptionsForWheel(wheelId: Long): List<WheelOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWheel(wheel: SavedWheelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<WheelOptionEntity>)

    @Query("DELETE FROM saved_wheel WHERE id = :id")
    suspend fun deleteWheel(id: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM saved_group ORDER BY createdAt DESC")
    fun getAllGroupsFlow(): Flow<List<SavedGroupEntity>>

    @Query("SELECT * FROM group_member WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: Long): List<GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SavedGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM saved_group WHERE id = :id")
    suspend fun deleteGroup(id: Long)
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM saved_countdown ORDER BY targetEpochDay ASC")
    fun getAllCountdownsFlow(): Flow<List<SavedCountdownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(countdown: SavedCountdownEntity): Long

    @Query("DELETE FROM saved_countdown WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM saved_template WHERE type = :type ORDER BY createdAt DESC")
    fun getTemplatesByTypeFlow(type: String): Flow<List<SavedTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: SavedTemplateEntity): Long

    @Query("DELETE FROM saved_template WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ToolUsageDao {
    @Query("SELECT * FROM tool_usage ORDER BY lastUsedAt DESC")
    fun getAllUsageFlow(): Flow<List<ToolUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordUsage(usage: ToolUsageEntity)
}
