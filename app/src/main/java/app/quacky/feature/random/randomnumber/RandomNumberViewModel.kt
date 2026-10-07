package app.quacky.feature.random.randomnumber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.repository.HistoryRepository
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

data class RngUiState(
    val min: Long = 1,
    val max: Long = 100,
    val count: Int = 1,
    val allowDuplicates: Boolean = true,
    val isSortResults: Boolean = false,
    val seed: String = "",
    val sequenceIndex: Long = 0,
    val generatedNumbers: List<Long> = listOf(42)
)

private data class RngConfig(
    val min: Long,
    val max: Long,
    val count: Int,
    val allowDuplicates: Boolean
)

private data class RngSequenceState(
    val isSortResults: Boolean,
    val seed: String,
    val sequenceIndex: Long
)

@HiltViewModel
class RandomNumberViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _min = MutableStateFlow(1L)
    private val _max = MutableStateFlow(100L)
    private val _count = MutableStateFlow(1)
    private val _allowDuplicates = MutableStateFlow(true)
    private val _isSortResults = MutableStateFlow(false)
    private val _seed = MutableStateFlow("")
    private val _sequenceIndex = MutableStateFlow(0L)
    private val _generatedNumbers = MutableStateFlow(listOf(42L))

    private val configFlow = combine(
        _min,
        _max,
        _count,
        _allowDuplicates
    ) { min, max, count, allowDupes ->
        RngConfig(min, max, count, allowDupes)
    }

    private val sequenceFlow = combine(
        _isSortResults,
        _seed,
        _sequenceIndex
    ) { sort, seed, seqIdx ->
        RngSequenceState(sort, seed, seqIdx)
    }

    val uiState: StateFlow<RngUiState> = combine(
        configFlow,
        sequenceFlow
    ) { config, seq ->
        RngUiState(
            min = config.min,
            max = config.max,
            count = config.count,
            allowDuplicates = config.allowDuplicates,
            isSortResults = seq.isSortResults,
            seed = seq.seed,
            sequenceIndex = seq.sequenceIndex,
            generatedNumbers = _generatedNumbers.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RngUiState())

    fun setRange(min: Long, max: Long) {
        _min.value = min
        _max.value = maxOf(min, max)
    }

    fun setMin(min: Long) {
        _min.value = min
        if (_max.value < min) {
            _max.value = min
        }
    }

    fun setMax(max: Long) {
        _max.value = max
        if (_min.value > max) {
            _min.value = max
        }
    }

    fun reset() {
        _min.value = 1L
        _max.value = 100L
        _count.value = 1
        _allowDuplicates.value = true
        _isSortResults.value = false
        _seed.value = ""
        _sequenceIndex.value = 0L
        _generatedNumbers.value = listOf(42L)
    }

    fun setCount(c: Int) {
        _count.value = c.coerceIn(1, 1000)
    }

    fun setAllowDuplicates(allow: Boolean) {
        _allowDuplicates.value = allow
    }

    fun setSortResults(sort: Boolean) {
        _isSortResults.value = sort
    }

    fun setSeed(s: String) {
        _seed.value = s
        _sequenceIndex.value = 0
    }

    fun generate(nextSequence: Boolean = false) {
        if (nextSequence && _seed.value.isNotBlank()) {
            _sequenceIndex.value++
        }

        var results = RngEngine.generateIntegers(
            min = _min.value,
            max = _max.value,
            count = _count.value,
            allowDuplicates = _allowDuplicates.value,
            seed = _seed.value.ifBlank { null },
            sequenceIndex = _sequenceIndex.value
        )

        if (_isSortResults.value) {
            results = results.sorted()
        }

        _generatedNumbers.value = results

        viewModelScope.launch {
            val title = if (results.size == 1) "Number: ${results.first()}" else "Generated ${results.size} numbers"
            val subtitle = results.take(6).joinToString(", ") + if (results.size > 6) "..." else ""
            val payload = JSONObject().apply {
                put("min", _min.value)
                put("max", _max.value)
                put("count", _count.value)
                put("seed", _seed.value)
                put("results", results.joinToString(","))
            }.toString()

            historyRepository.addEntry(
                toolId = ToolRegistry.RANDOM_NUMBER.id,
                type = "rng",
                title = title,
                subtitle = subtitle,
                payloadJson = payload
            )
        }
    }
}
