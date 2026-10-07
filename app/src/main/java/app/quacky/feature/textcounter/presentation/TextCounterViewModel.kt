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
    val ignoreCase: Boolean = true,
    val matchWholeWord: Boolean = false,
    val isRegex: Boolean = false,
    val searchMode: SearchMode = SearchMode.LETTER,
    val searchMatches: List<SearchMatch> = emptyList(),
    val customMatchCount: Int = 0,
    val customMatchPercentage: Float = 0f
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
    private val _ignoreCase = MutableStateFlow(true)
    private val _matchWholeWord = MutableStateFlow(false)
    private val _isRegex = MutableStateFlow(false)

    val uiState: StateFlow<TextCounterUiState> = combine(
        _text,
        _selectedPreset,
        _ignoreStopwords,
        _searchQuery,
        _ignoreCase,
        _matchWholeWord,
        _isRegex
    ) { args: Array<Any> ->
        val currentText = args[0] as String
        val preset = args[1] as CharacterPreset
        val ignoreStop = args[2] as Boolean
        val query = args[3] as String
        val ignCase = args[4] as Boolean
        val wholeWord = args[5] as Boolean
        val regexMode = args[6] as Boolean

        val stats = TextAnalyzer.analyze(currentText)
        val wordFreq = TextAnalyzer.computeWordFrequency(currentText, ignoreStopwords = ignoreStop)
        val letterFreq = TextAnalyzer.computeLetterFrequency(currentText)

        val mode = when {
            regexMode -> SearchMode.REGEX
            wholeWord -> SearchMode.WORD
            else -> SearchMode.LETTER
        }

        val matches = if (query.isNotBlank()) {
            TextAnalyzer.findOccurrences(
                text = currentText,
                query = query,
                mode = mode,
                caseSensitive = !ignCase
            )
        } else {
            emptyList()
        }

        val matchCount = matches.size
        val percentage = if (wholeWord) {
            if (stats.wordCount > 0) (matchCount.toFloat() / stats.wordCount) * 100f else 0f
        } else {
            if (stats.characterCountWithSpaces > 0) (matchCount.toFloat() / stats.characterCountWithSpaces) * 100f else 0f
        }

        TextCounterUiState(
            text = currentText,
            stats = stats,
            selectedPreset = preset,
            wordFrequencies = wordFreq,
            letterFrequencies = letterFreq,
            ignoreStopwords = ignoreStop,
            searchQuery = query,
            ignoreCase = ignCase,
            matchWholeWord = wholeWord,
            isRegex = regexMode,
            searchMode = mode,
            searchMatches = matches,
            customMatchCount = matchCount,
            customMatchPercentage = percentage
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

    fun toggleIgnoreCase() {
        _ignoreCase.value = !_ignoreCase.value
    }

    fun toggleMatchWholeWord() {
        _matchWholeWord.value = !_matchWholeWord.value
    }

    fun toggleRegex() {
        _isRegex.value = !_isRegex.value
    }

    fun clearText() {
        _text.value = ""
        _searchQuery.value = ""
    }

    fun reset() {
        _text.value = ""
        _searchQuery.value = ""
        _selectedPreset.value = CharacterPreset.NONE
        _ignoreCase.value = true
        _matchWholeWord.value = false
        _isRegex.value = false
        _ignoreStopwords.value = true
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
