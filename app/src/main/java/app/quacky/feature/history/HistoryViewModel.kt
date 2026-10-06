package app.quacky.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.entity.HistoryEntryEntity
import app.quacky.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val groupedEntries: Map<String, List<HistoryEntryEntity>> = emptyMap(),
    val totalCount: Int = 0,
    val selectedFilterToolId: String? = null,
    val selectedFilterCategory: ToolCategory? = null,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val selectedEntryIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false
)

private data class FilterOptions(
    val toolId: String?,
    val category: ToolCategory?,
    val onlyFavorites: Boolean,
    val searchQuery: String
)

private data class SelectionOptions(
    val selectedIds: Set<Long>,
    val isMultiSelect: Boolean
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _selectedToolId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedEntryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isMultiSelectMode = MutableStateFlow(false)

    private val filterOptionsFlow = combine(
        _selectedToolId,
        _selectedCategory,
        _onlyFavorites,
        _searchQuery
    ) { toolId, category, favorites, query ->
        FilterOptions(toolId, category, favorites, query)
    }

    private val selectionOptionsFlow = combine(
        _selectedEntryIds,
        _isMultiSelectMode
    ) { selectedIds, multiSelect ->
        SelectionOptions(selectedIds, multiSelect)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allHistory,
        filterOptionsFlow,
        selectionOptionsFlow
    ) { allEntries, filters, selection ->
        val filtered = allEntries.filter { entry ->
            val matchesTool = filters.toolId == null || entry.toolId == filters.toolId
            val matchesCategory = filters.category == null || ToolRegistry.getById(entry.toolId)?.category == filters.category
            val matchesFav = !filters.onlyFavorites || entry.isFavorite
            val matchesQuery = filters.searchQuery.isBlank() ||
                    entry.title.contains(filters.searchQuery, ignoreCase = true) ||
                    entry.subtitle.contains(filters.searchQuery, ignoreCase = true)
            matchesTool && matchesCategory && matchesFav && matchesQuery
        }

        val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        val grouped = filtered.groupBy { dateFormat.format(Date(it.createdAt)) }

        HistoryUiState(
            groupedEntries = grouped,
            totalCount = filtered.size,
            selectedFilterToolId = filters.toolId,
            selectedFilterCategory = filters.category,
            onlyFavorites = filters.onlyFavorites,
            searchQuery = filters.searchQuery,
            selectedEntryIds = selection.selectedIds,
            isMultiSelectMode = selection.isMultiSelect
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun setFilterTool(toolId: String?) {
        _selectedToolId.value = if (_selectedToolId.value == toolId) null else toolId
    }

    fun setFilterCategory(category: ToolCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }

    fun toggleSelectEntry(id: Long) {
        val current = _selectedEntryIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedEntryIds.value = current
        _isMultiSelectMode.value = current.isNotEmpty()
    }

    fun clearSelection() {
        _selectedEntryIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun deleteSelected() {
        val ids = _selectedEntryIds.value.toList()
        viewModelScope.launch {
            repository.deleteEntries(ids)
            clearSelection()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            clearSelection()
        }
    }
}
