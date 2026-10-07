package app.quacky.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quacky_settings")

val LocalAppPreferences = androidx.compose.runtime.staticCompositionLocalOf<AppPreferences?> { null }

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_PINNED_TOOLS = stringPreferencesKey("pinned_tools")
        val KEY_RECENT_TOOLS = stringPreferencesKey("recent_tools")
        val KEY_FLAT_GRID_HOME = booleanPreferencesKey("flat_grid_home")
        val KEY_SHOW_RECENTS = booleanPreferencesKey("show_recents")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_TIPS_ON_FIRST_OPEN = booleanPreferencesKey("tips_on_first_open")
        val KEY_RULER_CALIBRATION_FACTOR = floatPreferencesKey("ruler_calibration_factor")
        val KEY_RULER_IS_CALIBRATED = booleanPreferencesKey("ruler_is_calibrated")
        val KEY_EASTER_EGG_FOUND = booleanPreferencesKey("easter_egg_found")
        val KEY_HISTORY_MAX_ENTRIES = intPreferencesKey("history_max_entries")
        val KEY_HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val KEY_AR_RULER_UNIT = stringPreferencesKey("ar_ruler_unit")
        val KEY_AR_RULER_ACCURACY_NOTE_DISMISSED = booleanPreferencesKey("ar_ruler_accuracy_note_dismissed")
        val KEY_AR_AUTO_FINISH = booleanPreferencesKey("ar_auto_finish")
        val KEY_CONFIRM_ON_BACK = booleanPreferencesKey("confirm_on_back")
        val KEY_SURFER_HIGH_SCORE = intPreferencesKey("surfer_high_score")
        val KEY_SURFER_TOTAL_COINS = intPreferencesKey("surfer_total_coins")
        val KEY_SURFER_GAMES_PLAYED = intPreferencesKey("surfer_games_played")
        val KEY_SURFER_MAX_DISTANCE = intPreferencesKey("surfer_max_distance")
    }

    // Pinned Tools: stored as comma-separated or JSON list of IDs
    val pinnedToolIds: Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_PINNED_TOOLS] ?: "qr_scanner,screen_ruler,color_picker,text_counter"
        if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun setPinnedToolIds(ids: List<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_PINNED_TOOLS] = ids.joinToString(",")
        }
    }

    suspend fun togglePinTool(toolId: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_PINNED_TOOLS] ?: "qr_scanner,screen_ruler,color_picker,text_counter"
            val current = raw.split(",").filter { it.isNotBlank() }.toMutableList()
            if (current.contains(toolId)) {
                current.remove(toolId)
            } else {
                current.add(toolId)
            }
            prefs[KEY_PINNED_TOOLS] = current.joinToString(",")
        }
    }

    // Recent Tools (last 4 used tools)
    val recentToolIds: Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_TOOLS] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun recordToolUsed(toolId: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_TOOLS] ?: ""
            val current = raw.split(",").filter { it.isNotBlank() && it != toolId }.toMutableList()
            current.add(0, toolId)
            val trimmed = current.take(4)
            prefs[KEY_RECENT_TOOLS] = trimmed.joinToString(",")
        }
    }

    // Settings
    val isFlatGridHome: Flow<Boolean> = dataStore.data.map { it[KEY_FLAT_GRID_HOME] ?: false }
    suspend fun setFlatGridHome(value: Boolean) = dataStore.edit { it[KEY_FLAT_GRID_HOME] = value }

    val isShowRecents: Flow<Boolean> = dataStore.data.map { it[KEY_SHOW_RECENTS] ?: true }
    suspend fun setShowRecents(value: Boolean) = dataStore.edit { it[KEY_SHOW_RECENTS] = value }

    val isHapticsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    suspend fun setHapticsEnabled(value: Boolean) = dataStore.edit { it[KEY_HAPTICS_ENABLED] = value }

    val isTipsOnFirstOpen: Flow<Boolean> = dataStore.data.map { it[KEY_TIPS_ON_FIRST_OPEN] ?: true }
    suspend fun setTipsOnFirstOpen(value: Boolean) = dataStore.edit { it[KEY_TIPS_ON_FIRST_OPEN] = value }

    val isConfirmOnBackEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CONFIRM_ON_BACK] ?: true }
    suspend fun setConfirmOnBackEnabled(value: Boolean) = dataStore.edit { it[KEY_CONFIRM_ON_BACK] = value }

    // Ruler calibration
    val rulerCalibrationFactor: Flow<Float> = dataStore.data.map { it[KEY_RULER_CALIBRATION_FACTOR] ?: 1.0f }
    val isRulerCalibrated: Flow<Boolean> = dataStore.data.map { it[KEY_RULER_IS_CALIBRATED] ?: false }

    suspend fun setRulerCalibration(factor: Float) {
        dataStore.edit {
            it[KEY_RULER_CALIBRATION_FACTOR] = factor
            it[KEY_RULER_IS_CALIBRATED] = true
        }
    }

    suspend fun resetRulerCalibration() {
        dataStore.edit {
            it[KEY_RULER_CALIBRATION_FACTOR] = 1.0f
            it[KEY_RULER_IS_CALIBRATED] = false
        }
    }

    // AR Ruler
    val arRulerUnit: Flow<String> = dataStore.data.map { it[KEY_AR_RULER_UNIT] ?: "cm" }
    suspend fun setArRulerUnit(unit: String) = dataStore.edit { it[KEY_AR_RULER_UNIT] = unit }

    val isArAccuracyNoteDismissed: Flow<Boolean> = dataStore.data.map { it[KEY_AR_RULER_ACCURACY_NOTE_DISMISSED] ?: false }
    suspend fun setArAccuracyNoteDismissed(dismissed: Boolean) = dataStore.edit { it[KEY_AR_RULER_ACCURACY_NOTE_DISMISSED] = dismissed }

    // Easter Egg
    val isEasterEggFound: Flow<Boolean> = dataStore.data.map { it[KEY_EASTER_EGG_FOUND] ?: false }
    suspend fun setEasterEggFound(value: Boolean) = dataStore.edit { it[KEY_EASTER_EGG_FOUND] = value }

    // AR Ruler Auto-Finish
    val isArAutoFinishEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AR_AUTO_FINISH] ?: true // on by default
    }

    suspend fun setArAutoFinishEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AR_AUTO_FINISH] = enabled
        }
    }

    // Tips Seen per tool
    fun isTipSeen(toolId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("tip_seen_$toolId")
        return dataStore.data.map { it[key] ?: false }
    }

    suspend fun setTipSeen(toolId: String, seen: Boolean = true) {
        val key = booleanPreferencesKey("tip_seen_$toolId")
        dataStore.edit { it[key] = seen }
    }

    suspend fun resetAllTips() {
        dataStore.edit { prefs ->
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith("tip_seen_") }
            keysToRemove.forEach { key ->
                @Suppress("UNCHECKED_CAST")
                prefs.remove(key as Preferences.Key<Any>)
            }
        }
    }

    // Quacky Surfer Game Stats
    val surferHighScore: Flow<Int> = dataStore.data.map { it[KEY_SURFER_HIGH_SCORE] ?: 0 }
    val surferTotalCoins: Flow<Int> = dataStore.data.map { it[KEY_SURFER_TOTAL_COINS] ?: 0 }
    val surferGamesPlayed: Flow<Int> = dataStore.data.map { it[KEY_SURFER_GAMES_PLAYED] ?: 0 }
    val surferMaxDistance: Flow<Int> = dataStore.data.map { it[KEY_SURFER_MAX_DISTANCE] ?: 0 }

    suspend fun recordSurferGameResult(score: Int, coins: Int, distance: Int): Boolean {
        var isNewHighScore = false
        dataStore.edit { prefs ->
            val currentBest = prefs[KEY_SURFER_HIGH_SCORE] ?: 0
            if (score > currentBest) {
                prefs[KEY_SURFER_HIGH_SCORE] = score
                isNewHighScore = true
            }
            val currentCoins = prefs[KEY_SURFER_TOTAL_COINS] ?: 0
            prefs[KEY_SURFER_TOTAL_COINS] = currentCoins + coins

            val games = prefs[KEY_SURFER_GAMES_PLAYED] ?: 0
            prefs[KEY_SURFER_GAMES_PLAYED] = games + 1

            val currentMaxDist = prefs[KEY_SURFER_MAX_DISTANCE] ?: 0
            if (distance > currentMaxDist) {
                prefs[KEY_SURFER_MAX_DISTANCE] = distance
            }
        }
        return isNewHighScore
    }
}
