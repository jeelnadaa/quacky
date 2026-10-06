package app.quacky.feature.documentscanner.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.documentscanner.domain.CornerPoint
import app.quacky.feature.documentscanner.domain.DocFilterType
import app.quacky.feature.documentscanner.domain.DocumentProcessor
import app.quacky.feature.documentscanner.domain.DocumentQuad
import app.quacky.feature.documentscanner.domain.PdfGenerator
import app.quacky.feature.documentscanner.domain.ScannedPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class DocScanStep {
    CAPTURE,
    CROP,
    REVIEW
}

data class DocScannerUiState(
    val step: DocScanStep = DocScanStep.CAPTURE,
    val pages: List<ScannedPage> = emptyList(),
    val activePageIndex: Int = 0,
    val activeCropQuad: DocumentQuad = DocumentQuad(),
    val activeFilter: DocFilterType = DocFilterType.MAGIC_COLOR,
    val flashEnabled: Boolean = false,
    val isProcessing: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val generatedPdfFile: File? = null,
    val snackbarMessage: String? = null
) {
    val currentPage: ScannedPage?
        get() = pages.getOrNull(activePageIndex)
}

@HiltViewModel
class DocumentScannerViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocScannerUiState())
    val uiState: StateFlow<DocScannerUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(bitmap: Bitmap) {
        val nextIndex = _uiState.value.pages.size
        val newPage = ScannedPage(
            id = UUID.randomUUID().toString(),
            pageIndex = nextIndex,
            originalBitmap = bitmap,
            quad = DocumentQuad()
        )
        _uiState.update {
            it.copy(
                pages = it.pages + newPage,
                activePageIndex = nextIndex,
                activeCropQuad = newPage.quad,
                step = DocScanStep.CROP
            )
        }
    }

    fun onPhotosImported(bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return
        val existing = _uiState.value.pages.toMutableList()
        val startIndex = existing.size
        bitmaps.forEachIndexed { idx, bmp ->
            existing.add(
                ScannedPage(
                    id = UUID.randomUUID().toString(),
                    pageIndex = startIndex + idx,
                    originalBitmap = bmp,
                    quad = DocumentQuad()
                )
            )
        }
        _uiState.update {
            it.copy(
                pages = existing,
                activePageIndex = startIndex,
                activeCropQuad = existing[startIndex].quad,
                step = DocScanStep.CROP,
                snackbarMessage = "Imported ${bitmaps.size} photos"
            )
        }
    }

    fun updateCropCorner(corner: String, newPoint: CornerPoint) {
        _uiState.update { state ->
            val curQuad = state.activeCropQuad
            val updated = when (corner) {
                "TL" -> curQuad.copy(topLeft = newPoint)
                "TR" -> curQuad.copy(topRight = newPoint)
                "BR" -> curQuad.copy(bottomRight = newPoint)
                "BL" -> curQuad.copy(bottomLeft = newPoint)
                else -> curQuad
            }
            state.copy(activeCropQuad = updated)
        }
    }

    fun resetCropToFull() {
        _uiState.update {
            it.copy(
                activeCropQuad = DocumentQuad(
                    topLeft = CornerPoint(0f, 0f),
                    topRight = CornerPoint(1f, 0f),
                    bottomRight = CornerPoint(1f, 1f),
                    bottomLeft = CornerPoint(0f, 1f)
                )
            )
        }
    }

    fun resetCropToInset() {
        _uiState.update {
            it.copy(activeCropQuad = DocumentQuad())
        }
    }

    fun applyCrop() {
        val state = _uiState.value
        val curPage = state.currentPage ?: return

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val warped = DocumentProcessor.warpPerspective(curPage.originalBitmap, state.activeCropQuad)
            val filtered = DocumentProcessor.applyFilter(warped, curPage.filterType)

            withContext(Dispatchers.Main) {
                val updatedPages = state.pages.toMutableList()
                val updatedPage = curPage.copy(
                    quad = state.activeCropQuad,
                    warpedBitmap = filtered
                )
                updatedPages[state.activePageIndex] = updatedPage

                _uiState.update {
                    it.copy(
                        pages = updatedPages,
                        isProcessing = false,
                        step = DocScanStep.REVIEW
                    )
                }
            }
        }
    }

    fun setFilter(filter: DocFilterType) {
        val state = _uiState.value
        val curPage = state.currentPage ?: return
        if (curPage.warpedBitmap == null) return

        _uiState.update { it.copy(isProcessing = true, activeFilter = filter) }

        viewModelScope.launch(Dispatchers.Default) {
            // Re-apply from warped base
            val baseWarped = DocumentProcessor.warpPerspective(curPage.originalBitmap, curPage.quad)
            val rotated = DocumentProcessor.rotateBitmap(baseWarped, curPage.rotationDegrees)
            val filtered = DocumentProcessor.applyFilter(rotated, filter)

            withContext(Dispatchers.Main) {
                val updatedPages = state.pages.toMutableList()
                updatedPages[state.activePageIndex] = curPage.copy(
                    filterType = filter,
                    warpedBitmap = filtered
                )
                _uiState.update {
                    it.copy(pages = updatedPages, isProcessing = false)
                }
            }
        }
    }

    fun rotateCurrentPage() {
        val state = _uiState.value
        val curPage = state.currentPage ?: return
        val nextDegrees = (curPage.rotationDegrees + 90) % 360

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val baseWarped = DocumentProcessor.warpPerspective(curPage.originalBitmap, curPage.quad)
            val rotated = DocumentProcessor.rotateBitmap(baseWarped, nextDegrees)
            val filtered = DocumentProcessor.applyFilter(rotated, curPage.filterType)

            withContext(Dispatchers.Main) {
                val updatedPages = state.pages.toMutableList()
                updatedPages[state.activePageIndex] = curPage.copy(
                    rotationDegrees = nextDegrees,
                    warpedBitmap = filtered
                )
                _uiState.update {
                    it.copy(pages = updatedPages, isProcessing = false)
                }
            }
        }
    }

    fun selectPage(index: Int) {
        val state = _uiState.value
        if (index in state.pages.indices) {
            val page = state.pages[index]
            _uiState.update {
                it.copy(
                    activePageIndex = index,
                    activeCropQuad = page.quad,
                    activeFilter = page.filterType
                )
            }
        }
    }

    fun retakeOrRecropCurrentPage() {
        val page = _uiState.value.currentPage ?: return
        _uiState.update {
            it.copy(
                activeCropQuad = page.quad,
                step = DocScanStep.CROP
            )
        }
    }

    fun deleteCurrentPage() {
        val state = _uiState.value
        if (state.pages.isEmpty()) return

        val updated = state.pages.filterIndexed { idx, _ -> idx != state.activePageIndex }
        if (updated.isEmpty()) {
            _uiState.update {
                it.copy(
                    pages = emptyList(),
                    activePageIndex = 0,
                    step = DocScanStep.CAPTURE
                )
            }
        } else {
            val nextActive = (state.activePageIndex - 1).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    pages = updated,
                    activePageIndex = nextActive,
                    activeCropQuad = updated[nextActive].quad,
                    activeFilter = updated[nextActive].filterType,
                    snackbarMessage = "Page removed"
                )
            }
        }
    }

    fun goToCapture() {
        _uiState.update { it.copy(step = DocScanStep.CAPTURE) }
    }

    fun toggleFlash() {
        _uiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun exportPdf(context: Context, docName: String? = null) {
        val state = _uiState.value
        val pages = state.pages
        if (pages.isEmpty()) return

        _uiState.update { it.copy(isGeneratingPdf = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val baseName = docName?.trim()?.ifBlank { null } ?: "Document_$timeStamp"
            val pdfFile = File(context.cacheDir, "scans/$baseName.pdf")

            val bitmapsToCompile = pages.mapNotNull { it.warpedBitmap ?: it.originalBitmap }
            val result = PdfGenerator.generatePdf(bitmapsToCompile, pdfFile)

            result.fold(
                onSuccess = { file ->
                    val sizeKb = file.length() / 1024

                    // Create thumbnail of first page for Room History
                    val firstBmp = bitmapsToCompile.firstOrNull()
                    val thumbBytes = firstBmp?.let { bmp ->
                        val out = ByteArrayOutputStream()
                        val thumb = Bitmap.createScaledBitmap(bmp, 200, 280, true)
                        thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        out.toByteArray()
                    }

                    val payload = JSONObject().apply {
                        put("fileName", file.name)
                        put("filePath", file.absolutePath)
                        put("pagesCount", pages.size)
                        put("fileSizeBytes", file.length())
                        put("timestamp", System.currentTimeMillis())
                    }.toString()

                    historyRepository.addEntry(
                        toolId = ToolRegistry.DOCUMENT_SCANNER.id,
                        type = "PDF",
                        title = file.nameWithoutExtension,
                        subtitle = "${pages.size} pages · $sizeKb KB",
                        payloadJson = payload,
                        thumbnailBytes = thumbBytes
                    )

                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isGeneratingPdf = false,
                                generatedPdfFile = file,
                                snackbarMessage = "PDF created: ${file.name} ($sizeKb KB)"
                            )
                        }
                    }
                },
                onFailure = { err ->
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isGeneratingPdf = false,
                                snackbarMessage = "Failed to create PDF: ${err.message}"
                            )
                        }
                    }
                }
            )
        }
    }

    fun clearGeneratedPdf() {
        _uiState.update { it.copy(generatedPdfFile = null) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
