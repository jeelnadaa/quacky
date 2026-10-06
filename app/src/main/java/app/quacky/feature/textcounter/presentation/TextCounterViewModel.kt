package app.quacky.feature.textcounter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.textcounter.domain.LetterFrequency
import app.quacky.feature.textcounter.domain.SearchMatch
import app.quacky.feature.textcounter.domain.SearchMode
import app.quacky.feature.textcounter.domain.TextAnalyzer
import app.quacky.feature.textcounter.domain.TextStatistics
import app.quacky.feature.textcounter.domain.WordFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

enum class CharacterPreset(val label: String, val limit: Int) {
    NONE("None", 0),
    TWITTER("X / Twitter", 280),
    INSTAGRAM_BIO("IG Bio", 150),
    INSTAGRAM_CAPTION("IG Caption", 2200),
    SMS("SMS (1 segment)", 160),
    META_DESC("Meta Desc", 160),
    SEO_TITLE("SEO Title", 60),
    LINKEDIN("LinkedIn", 3000)
}

data class TextCounterUiState(
    val text: String = "",
    val stats: TextStatistics = TextStatistics(),
    val selectedPreset: CharacterPreset = CharacterPreset.NONE,
    val wordFrequencies: List<WordFrequency> = emptyList(),
    val letterFrequencies: List<LetterFrequency> = emptyList(),
    val ignoreStopwords: Boolean = true,
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.WORD,
    val searchMatches: List<SearchMatch> = emptyList()
)

@HiltViewModel
class TextCounterViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _selectedPreset = MutableStateFlow(CharacterPreset.NONE)
    private val _ignoreStopwords = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _searchMode = MutableStateFlow(SearchMode.WORD)

    val uiState: StateFlow<TextCounterUiState> = combine(
        _text,
        _selectedPreset,
        _ignoreStopwords,
        _searchQuery,
        _searchMode
    ) { currentText, preset, ignoreStop, query, mode ->
        val stats = TextAnalyzer.analyze(currentText)
        val wordFreq = TextAnalyzer.computeWordFrequency(currentText, ignoreStopwords = ignoreStop)
        val letterFreq = TextAnalyzer.computeLetterFrequency(currentText)
        val matches = if (query.isNotBlank()) {
            TextAnalyzer.findOccurrences(currentText, query, mode)
        } else {
            emptyList()
        }

        TextCounterUiState(
            text = currentText,
            stats = stats,
            selectedPreset = preset,
            wordFrequencies = wordFreq,
            letterFrequencies = letterFreq,
            ignoreStopwords = ignoreStop,
            searchQuery = query,
            searchMode = mode,
            searchMatches = matches
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TextCounterUiState()
    )

    fun onTextChanged(newText: String) {
        _text.value = newText
    }

    fun selectPreset(preset: CharacterPreset) {
        _selectedPreset.value = if (_selectedPreset.value == preset) CharacterPreset.NONE else preset
    }

    fun toggleIgnoreStopwords() {
        _ignoreStopwords.value = !_ignoreStopwords.value
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSearchMode(mode: SearchMode) {
        _searchMode.value = mode
    }

    fun clearText() {
        _text.value = ""
        _searchQuery.value = ""
    }

    fun saveSnippetToHistory() {
        val current = _text.value
        if (current.isBlank()) return

        val title = current.take(40).trim()
        val stats = TextAnalyzer.analyze(current)
        val subtitle = "${stats.wordCount} words · ${stats.characterCountWithSpaces} chars"
        val payload = JSONObject().apply {
            put("fullText", current)
            put("wordCount", stats.wordCount)
            put("charCount", stats.characterCountWithSpaces)
            put("readingTime", stats.readingTimeSeconds)
        }.toString()

        viewModelScope.launch {
            historyRepository.addEntry(
                toolId = ToolRegistry.TEXT_COUNTER.id,
                type = "snippet",
                title = title,
                subtitle = subtitle,
                payloadJson = payload
            )
        }
    }
}
