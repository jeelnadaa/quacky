package app.quacky.feature.metadata.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.local.db.entity.HistoryEntryEntity
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.metadata.domain.CleanResult
import app.quacky.feature.metadata.domain.CustomRemovalOptions
import app.quacky.feature.metadata.domain.ExifEngine
import app.quacky.feature.metadata.domain.MetadataStripper
import app.quacky.feature.metadata.domain.PhotoMetadata
import app.quacky.feature.metadata.domain.PrivacyScore
import app.quacky.feature.metadata.domain.RemovalChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

data class MetadataUiState(
    val photos: List<PhotoMetadata> = emptyList(),
    val activeIndex: Int = 0,
    val isLoading: Boolean = false,
    val isCleaning: Boolean = false,
    val cleaningProgress: String = "",
    val removalChoice: RemovalChoice = RemovalChoice.ALL,
    val customOptions: CustomRemovalOptions = CustomRemovalOptions(),
    val showCleanSheet: Boolean = false,
    val tagSearchQuery: String = "",
    val lastCleanResults: List<CleanResult> = emptyList(),
    val cleanCompletionMessage: String? = null,
    val userErrorMessage: String? = null
) {
    val activePhoto: PhotoMetadata?
        get() = if (photos.indices.contains(activeIndex)) photos[activeIndex] else null
}

@HiltViewModel
class MetadataViewModel @Inject constructor(
    application: Application,
    private val historyRepository: HistoryRepository,
    private val capabilities: app.quacky.core.capability.DeviceCapabilities
) : AndroidViewModel(application) {

    fun canOpenMaps(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=0,0"))
        return capabilities.canHandle(intent)
    }

    private val _uiState = MutableStateFlow(MetadataUiState())
    val uiState: StateFlow<MetadataUiState> = _uiState.asStateFlow()

    private val stripper = MetadataStripper(application)

    fun loadPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userErrorMessage = null) }
            val context = getApplication<Application>()

            val parsedList = withContext(Dispatchers.IO) {
                val list = mutableListOf<PhotoMetadata>()
                for (uri in uris) {
                    try {
                        val (fileName, fileSize) = ExifEngine.getFileDetails(context, uri)
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val meta = ExifEngine.parse(stream, uri, fileName, fileSize, mimeType)
                            list.add(meta)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                list
            }

            _uiState.update { current ->
                val combined = (current.photos + parsedList).distinctBy { it.uri }
                current.copy(
                    photos = combined,
                    activeIndex = if (current.photos.isEmpty() && combined.isNotEmpty()) 0 else current.activeIndex,
                    isLoading = false
                )
            }
        }
    }

    fun selectActiveIndex(index: Int) {
        if (index in _uiState.value.photos.indices) {
            _uiState.update { it.copy(activeIndex = index) }
        }
    }

    fun removePhoto(index: Int) {
        _uiState.update { state ->
            val updated = state.photos.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            val newIndex = if (updated.isEmpty()) 0 else (state.activeIndex.coerceAtMost(updated.size - 1))
            state.copy(photos = updated, activeIndex = newIndex)
        }
    }

    fun clearAllPhotos() {
        _uiState.update { it.copy(photos = emptyList(), activeIndex = 0, lastCleanResults = emptyList()) }
    }

    fun setRemovalChoice(choice: RemovalChoice) {
        _uiState.update { it.copy(removalChoice = choice) }
    }

    fun updateCustomOptions(options: CustomRemovalOptions) {
        _uiState.update { it.copy(customOptions = options) }
    }

    fun setShowCleanSheet(show: Boolean) {
        _uiState.update { it.copy(showCleanSheet = show) }
    }

    fun setTagSearchQuery(query: String) {
        _uiState.update { it.copy(tagSearchQuery = query) }
    }

    fun clearCompletionMessage() {
        _uiState.update { it.copy(cleanCompletionMessage = null) }
    }

    fun cleanPhotos(cleanAll: Boolean) {
        val currentState = _uiState.value
        val targets = if (cleanAll) currentState.photos else listOfNotNull(currentState.activePhoto)
        if (targets.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCleaning = true,
                    cleaningProgress = "Starting cleaning…",
                    showCleanSheet = false
                )
            }

            val results = mutableListOf<CleanResult>()

            withContext(Dispatchers.IO) {
                for ((idx, photo) in targets.withIndex()) {
                    _uiState.update {
                        it.copy(cleaningProgress = "Cleaning photo ${idx + 1} of ${targets.size}…")
                    }

                    try {
                        val uri = photo.uri ?: continue
                        val result = stripper.cleanImage(
                            uri = uri,
                            originalFileName = photo.fileName,
                            mimeType = photo.mimeType,
                            choice = currentState.removalChoice,
                            customOptions = currentState.customOptions,
                            onProgress = { step ->
                                _uiState.update { s -> s.copy(cleaningProgress = step) }
                            }
                        )
                        results.add(result)

                        // Privacy-safe history logging (NEVER store metadata values!)
                        val choiceDesc = when (currentState.removalChoice) {
                            RemovalChoice.ALL -> "All metadata"
                            RemovalChoice.LOCATION_ONLY -> "Location only"
                            RemovalChoice.CUSTOM -> "Custom selection"
                        }

                        val payload = JSONObject().apply {
                            put("fileName", photo.fileName)
                            put("removedCategories", choiceDesc)
                            put("tagsRemoved", result.tagsRemoved)
                            put("isLossless", result.isLossless)
                            put("verifiedClean", result.verifiedClean)
                        }.toString()

                        historyRepository.addEntry(
                            toolId = "metadata",
                            type = "CLEANED",
                            title = photo.fileName,
                            subtitle = "$choiceDesc • ${result.tagsRemoved} tags removed",
                            payloadJson = payload,
                            thumbnailBytes = null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val summary = if (results.size == 1) {
                val r = results.first()
                "Cleaned ${r.originalFileName} • ${r.tagsRemoved} tags removed (${if (r.isLossless) "Lossless" else "Re-encoded"})"
            } else {
                "${results.size} photos cleaned and verified."
            }

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    cleaningProgress = "",
                    lastCleanResults = results,
                    cleanCompletionMessage = summary
                )
            }
        }
    }

    fun shareCleanedPhoto(context: Context, result: CleanResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, result.outputUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share clean photo"))
    }
}
