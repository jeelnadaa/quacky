package app.quacky.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import app.quacky.core.util.SearchMatcher
import app.quacky.data.local.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val pinnedTools: List<ToolDefinition> = emptyList(),
    val recentTools: List<ToolDefinition> = emptyList(),
    val categories: List<ToolCategory> = ToolCategory.entries,
    val allTools: List<ToolDefinition> = ToolRegistry.allTools,
    val isFlatGrid: Boolean = false,
    val isShowRecents: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<ToolDefinition> = emptyList(),
    val isSearchActive: Boolean = false
)

private data class HomePrefs(
    val pinnedIds: List<String>,
    val recentIds: List<String>,
    val flatGrid: Boolean,
    val showRecents: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val prefsFlow = combine(
        preferences.pinnedToolIds,
        preferences.recentToolIds,
        preferences.isFlatGridHome,
        preferences.isShowRecents
    ) { pinnedIds, recentIds, flatGrid, showRecents ->
        HomePrefs(pinnedIds, recentIds, flatGrid, showRecents)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        prefsFlow,
        _searchQuery,
        _isSearchActive
    ) { prefs, query, searchActive ->
        val pinned = prefs.pinnedIds.mapNotNull { ToolRegistry.getById(it) }
        val recents = prefs.recentIds.mapNotNull { ToolRegistry.getById(it) }
        val results = if (query.isNotBlank()) {
            SearchMatcher.searchTools(context, query)
        } else {
            emptyList()
        }

        HomeUiState(
            pinnedTools = pinned,
            recentTools = recents,
            categories = ToolCategory.entries,
            allTools = ToolRegistry.allTools,
            isFlatGrid = prefs.flatGrid,
            isShowRecents = prefs.showRecents,
            searchQuery = query,
            searchResults = results,
            isSearchActive = searchActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun togglePin(toolId: String) {
        viewModelScope.launch {
            preferences.togglePinTool(toolId)
        }
    }

    fun recordToolUsed(toolId: String) {
        viewModelScope.launch {
            preferences.recordToolUsed(toolId)
        }
    }
}
