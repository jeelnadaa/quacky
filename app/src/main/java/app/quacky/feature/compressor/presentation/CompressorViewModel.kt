package app.quacky.feature.compressor.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.compressor.domain.CompressibleFile
import app.quacky.feature.compressor.domain.CompressionMode
import app.quacky.feature.compressor.domain.CompressionResult
import app.quacky.feature.compressor.domain.CompressorPreset
import app.quacky.feature.compressor.domain.ImageCompressEngine
import app.quacky.feature.compressor.domain.ImageCompressionConfig
import app.quacky.feature.compressor.domain.OutputFormat
import app.quacky.feature.compressor.domain.PdfCompressEngine
import app.quacky.feature.compressor.domain.PdfCompressionConfig
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

data class CompressorUiState(
    val files: List<CompressibleFile> = emptyList(),
    val activeIndex: Int = 0,
    val imageConfig: ImageCompressionConfig = ImageCompressionConfig(),
    val pdfConfig: PdfCompressionConfig = PdfCompressionConfig(),
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val results: List<CompressionResult> = emptyList(),
    val completionSummary: String? = null
) {
    val activeFile: CompressibleFile?
        get() = if (files.indices.contains(activeIndex)) files[activeIndex] else null

    val activeResult: CompressionResult?
        get() = results.firstOrNull { it.originalFileName == activeFile?.fileName }
}

@HiltViewModel
class CompressorViewModel @Inject constructor(
    application: Application,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CompressorUiState())
    val uiState: StateFlow<CompressorUiState> = _uiState.asStateFlow()

    private val imageEngine = ImageCompressEngine(application)
    private val pdfEngine = PdfCompressEngine(application)

    fun loadFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val context = getApplication<Application>()
            val parsedFiles = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        var name = uri.lastPathSegment ?: "file"
                        var size = 0L
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                                if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                            }
                        }
                        val mime = context.contentResolver.getType(uri) ?: ""
                        val isPdf = mime.equals("application/pdf", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true)

                        CompressibleFile(
                            uri = uri,
                            fileName = name,
                            originalSizeBytes = size,
                            mimeType = mime,
                            isPdf = isPdf,
                            formattedOriginalSize = CompressionResult.formatBytes(size)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }

            _uiState.update { current ->
                val combined = (current.files + parsedFiles).distinctBy { it.uri }
                current.copy(
                    files = combined,
                    activeIndex = if (current.files.isEmpty() && combined.isNotEmpty()) 0 else current.activeIndex
                )
            }
        }
    }

    fun selectActiveIndex(index: Int) {
        if (index in _uiState.value.files.indices) {
            _uiState.update { it.copy(activeIndex = index) }
        }
    }

    fun clearFiles() {
        _uiState.update { it.copy(files = emptyList(), activeIndex = 0, results = emptyList(), completionSummary = null) }
    }

    fun setCompressionMode(mode: CompressionMode) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(mode = mode)) }
    }

    fun setQuality(quality: Int) {
        _uiState.update {
            it.copy(
                imageConfig = it.imageConfig.copy(
                    quality = quality,
                    preset = CompressorPreset.CUSTOM
                )
            )
        }
    }

    fun setTargetSizeKb(targetKb: Long) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(targetSizeKb = targetKb)) }
    }

    fun setPreset(preset: CompressorPreset) {
        _uiState.update {
            it.copy(
                imageConfig = it.imageConfig.copy(
                    preset = preset,
                    quality = preset.quality,
                    resizeScale = preset.scale
                )
            )
        }
    }

    fun setOutputFormat(format: OutputFormat) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(outputFormat = format)) }
    }

    fun toggleStripMetadata(strip: Boolean) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(stripMetadata = strip)) }
    }

    fun toggleKeepDimensions(keep: Boolean) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(keepDimensions = keep)) }
    }

    fun setPdfDpi(dpi: Int) {
        _uiState.update { it.copy(pdfConfig = it.pdfConfig.copy(dpi = dpi)) }
    }

    fun setPdfPageQuality(quality: Int) {
        _uiState.update { it.copy(pdfConfig = it.pdfConfig.copy(pageQuality = quality)) }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(completionSummary = null) }
    }

    fun compressFiles(compressAll: Boolean) {
        val currentState = _uiState.value
        val targets = if (compressAll) currentState.files else listOfNotNull(currentState.activeFile)
        if (targets.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progressMessage = "Starting compression…") }

            val results = mutableListOf<CompressionResult>()

            withContext(Dispatchers.IO) {
                for ((idx, file) in targets.withIndex()) {
                    _uiState.update {
                        it.copy(progressMessage = "Compressing ${idx + 1} of ${targets.size}: ${file.fileName}…")
                    }

                    try {
                        val result = if (file.isPdf) {
                            pdfEngine.compressPdf(
                                uri = file.uri,
                                fileName = file.fileName,
                                originalSize = file.originalSizeBytes,
                                config = currentState.pdfConfig,
                                onProgress = { page, totalPages ->
                                    _uiState.update {
                                        it.copy(progressMessage = "${file.fileName} (Page $page of $totalPages)…")
                                    }
                                }
                            )
                        } else {
                            imageEngine.compressImage(
                                uri = file.uri,
                                fileName = file.fileName,
                                originalSize = file.originalSizeBytes,
                                config = currentState.imageConfig,
                                onProgress = { msg ->
                                    _uiState.update { it.copy(progressMessage = msg) }
                                }
                            )
                        }

                        results.add(result)

                        // Save to Room history
                        val payload = JSONObject().apply {
                            put("fileName", file.fileName)
                            put("isPdf", file.isPdf)
                            put("originalSize", file.originalSizeBytes)
                            put("compressedSize", result.compressedSizeBytes)
                            put("percentSaved", result.percentSaved)
                            put("isSmaller", result.isSmaller)
                        }.toString()

                        val subtitle = if (result.isSmaller) {
                            "Saved ${result.percentSaved}% (${result.formattedOriginalSize} → ${result.formattedCompressedSize})"
                        } else {
                            "Original kept (${result.formattedOriginalSize})"
                        }

                        historyRepository.addEntry(
                            toolId = "compressor",
                            type = if (file.isPdf) "PDF" else "IMAGE",
                            title = file.fileName,
                            subtitle = subtitle,
                            payloadJson = payload,
                            thumbnailBytes = null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val summary = if (results.size == 1) {
                results.first().message
            } else {
                "${results.size} files processed successfully."
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    progressMessage = "",
                    results = results,
                    completionSummary = summary
                )
            }
        }
    }

    fun shareResult(context: Context, result: CompressionResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (result.isPdf) "application/pdf" else "image/*"
            putExtra(Intent.EXTRA_STREAM, result.outputUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share compressed file"))
    }
}
