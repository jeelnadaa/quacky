package app.quacky.data.local.db

import android.content.Context
import androidx.room.Room
import app.quacky.data.local.db.dao.CountdownDao
import app.quacky.data.local.db.dao.GroupDao
import app.quacky.data.local.db.dao.HistoryDao
import app.quacky.data.local.db.dao.PaletteDao
import app.quacky.data.local.db.dao.TemplateDao
import app.quacky.data.local.db.dao.ToolUsageDao
import app.quacky.data.local.db.dao.WheelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): QuackyDatabase {
        return Room.databaseBuilder(
            context,
            QuackyDatabase::class.java,
            "quacky.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHistoryDao(db: QuackyDatabase): HistoryDao = db.historyDao()

    @Provides
    fun providePaletteDao(db: QuackyDatabase): PaletteDao = db.paletteDao()

    @Provides
    fun provideWheelDao(db: QuackyDatabase): WheelDao = db.wheelDao()

    @Provides
    fun provideGroupDao(db: QuackyDatabase): GroupDao = db.groupDao()

    @Provides
    fun provideCountdownDao(db: QuackyDatabase): CountdownDao = db.countdownDao()

    @Provides
    fun provideTemplateDao(db: QuackyDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideToolUsageDao(db: QuackyDatabase): ToolUsageDao = db.toolUsageDao()
}
