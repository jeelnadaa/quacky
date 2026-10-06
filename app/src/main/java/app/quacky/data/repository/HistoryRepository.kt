package app.quacky.data.repository

import android.content.Context
import app.quacky.data.local.db.dao.HistoryDao
import app.quacky.data.local.db.entity.HistoryEntryEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao
) {
    val allHistory: Flow<List<HistoryEntryEntity>> = historyDao.getAllHistoryFlow()
    val favoriteHistory: Flow<List<HistoryEntryEntity>> = historyDao.getFavoritesFlow()

    fun getHistoryForTool(toolId: String): Flow<List<HistoryEntryEntity>> =
        historyDao.getHistoryForToolFlow(toolId)

    suspend fun addEntry(
        toolId: String,
        type: String,
        title: String,
        subtitle: String,
        payloadJson: String,
        thumbnailBytes: ByteArray? = null
    ): Long = withContext(Dispatchers.IO) {
        var thumbnailPath: String? = null
        if (thumbnailBytes != null) {
            val thumbDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}_${(1000..9999).random()}.png")
            thumbFile.writeBytes(thumbnailBytes)
            thumbnailPath = thumbFile.absolutePath
        }

        val entity = HistoryEntryEntity(
            toolId = toolId,
            type = type,
            title = title,
            subtitle = subtitle,
            payloadJson = payloadJson,
            thumbnailPath = thumbnailPath
        )
        historyDao.insert(entity)
    }

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        historyDao.toggleFavorite(id)
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        val entry = historyDao.getById(id)
        entry?.thumbnailPath?.let { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        historyDao.deleteById(id)
    }

    suspend fun deleteEntries(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val entry = historyDao.getById(id)
            entry?.thumbnailPath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
        }
        historyDao.deleteByIds(ids)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            val thumbDir = File(context.filesDir, "thumbnails")
            thumbDir.deleteRecursively()
        } catch (_: Exception) {}
        historyDao.clearAll()
    }

    suspend fun exportToJson(entries: List<HistoryEntryEntity>): String = withContext(Dispatchers.Default) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("toolId", entry.toolId)
                put("type", entry.type)
                put("title", entry.title)
                put("subtitle", entry.subtitle)
                put("payloadJson", entry.payloadJson)
                put("createdAt", entry.createdAt)
                put("isFavorite", entry.isFavorite)
            }
            array.put(obj)
        }
        array.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val array = JSONArray(jsonString)
        var count = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val entry = HistoryEntryEntity(
                toolId = obj.getString("toolId"),
                type = obj.getString("type"),
                title = obj.getString("title"),
                subtitle = obj.optString("subtitle", ""),
                payloadJson = obj.optString("payloadJson", "{}"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                isFavorite = obj.optBoolean("isFavorite", false)
            )
            historyDao.insert(entry)
            count++
        }
        count
    }
}
