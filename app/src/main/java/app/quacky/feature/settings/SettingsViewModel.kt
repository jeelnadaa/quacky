package app.quacky.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isFlatGrid: Boolean = false,
    val isShowRecents: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isTipsOnFirstOpen: Boolean = true,
    val isEasterEggFound: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.isFlatGridHome,
        preferences.isShowRecents,
        preferences.isHapticsEnabled,
        preferences.isTipsOnFirstOpen,
        preferences.isEasterEggFound
    ) { flatGrid, showRecents, haptics, tipsOnFirstOpen, easterEgg ->
        SettingsUiState(
            isFlatGrid = flatGrid,
            isShowRecents = showRecents,
            isHapticsEnabled = haptics,
            isTipsOnFirstOpen = tipsOnFirstOpen,
            isEasterEggFound = easterEgg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setFlatGrid(value: Boolean) {
        viewModelScope.launch { preferences.setFlatGridHome(value) }
    }

    fun setShowRecents(value: Boolean) {
        viewModelScope.launch { preferences.setShowRecents(value) }
    }

    fun setHapticsEnabled(value: Boolean) {
        viewModelScope.launch { preferences.setHapticsEnabled(value) }
    }

    fun setTipsOnFirstOpen(value: Boolean) {
        viewModelScope.launch { preferences.setTipsOnFirstOpen(value) }
    }

    fun resetAllTips() {
        viewModelScope.launch { preferences.resetAllTips() }
    }

    fun clearAllHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    suspend fun exportHistoryJson(): String {
        val entries = historyRepository.allHistory.first()
        return historyRepository.exportToJson(entries)
    }

    suspend fun importHistoryJson(json: String): Int {
        return historyRepository.importFromJson(json)
    }

    fun onEasterEggUnlocked() {
        viewModelScope.launch { preferences.setEasterEggFound(true) }
    }
}
