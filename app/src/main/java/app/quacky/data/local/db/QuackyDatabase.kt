package app.quacky.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.quacky.data.local.db.dao.CountdownDao
import app.quacky.data.local.db.dao.GroupDao
import app.quacky.data.local.db.dao.HistoryDao
import app.quacky.data.local.db.dao.PaletteDao
import app.quacky.data.local.db.dao.TemplateDao
import app.quacky.data.local.db.dao.ToolUsageDao
import app.quacky.data.local.db.dao.WheelDao
import app.quacky.data.local.db.entity.GroupMemberEntity
import app.quacky.data.local.db.entity.HistoryEntryEntity
import app.quacky.data.local.db.entity.PaletteColorEntity
import app.quacky.data.local.db.entity.SavedCountdownEntity
import app.quacky.data.local.db.entity.SavedGroupEntity
import app.quacky.data.local.db.entity.SavedPaletteEntity
import app.quacky.data.local.db.entity.SavedTemplateEntity
import app.quacky.data.local.db.entity.SavedWheelEntity
import app.quacky.data.local.db.entity.SearchHistoryEntity
import app.quacky.data.local.db.entity.ToolUsageEntity
import app.quacky.data.local.db.entity.WheelOptionEntity

@Database(
    entities = [
        HistoryEntryEntity::class,
        SavedPaletteEntity::class,
        PaletteColorEntity::class,
        SavedWheelEntity::class,
        WheelOptionEntity::class,
        SavedGroupEntity::class,
        GroupMemberEntity::class,
        SavedCountdownEntity::class,
        SavedTemplateEntity::class,
        SearchHistoryEntity::class,
        ToolUsageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class QuackyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun paletteDao(): PaletteDao
    abstract fun wheelDao(): WheelDao
    abstract fun groupDao(): GroupDao
    abstract fun countdownDao(): CountdownDao
    abstract fun templateDao(): TemplateDao
    abstract fun toolUsageDao(): ToolUsageDao
}
