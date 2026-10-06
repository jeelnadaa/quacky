package app.quacky.feature.qrscanner.presentation

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.capability.DeviceCapabilities
import app.quacky.data.local.db.entity.HistoryEntryEntity
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.qrscanner.domain.BarcodeParser
import app.quacky.feature.qrscanner.domain.BarcodeType
import app.quacky.feature.qrscanner.domain.ParsedBarcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class QrScannerUiState(
    val hasFlash: Boolean = false,
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val activeResult: ParsedBarcode? = null,
    val showHistorySheet: Boolean = false,
    val historyList: List<HistoryEntryEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository,
    private val capabilities: DeviceCapabilities
) : ViewModel() {

    private val _isTorchOn = MutableStateFlow(false)
    private val _zoomRatio = MutableStateFlow(1.0f)
    private val _activeResult = MutableStateFlow<ParsedBarcode?>(null)
    private val _showHistorySheet = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private var lastScannedRaw: String? = null
    private var lastScannedTimestamp: Long = 0L

    val uiState: StateFlow<QrScannerUiState> = combine(
        _isTorchOn,
        _zoomRatio,
        _activeResult,
        _showHistorySheet,
        historyRepository.getHistoryForTool("qr_scanner")
    ) { torch, zoom, result, showHistory, historyList ->
        QrScannerUiState(
            hasFlash = capabilities.hasFlashUnit,
            isTorchOn = torch,
            zoomRatio = zoom,
            activeResult = result,
            showHistorySheet = showHistory,
            historyList = historyList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QrScannerUiState(hasFlash = capabilities.hasFlashUnit)
    )

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("qr_scanner")
        }
    }

    fun toggleTorch() {
        if (capabilities.hasFlashUnit) {
            _isTorchOn.value = !_isTorchOn.value
        }
    }

    fun setZoomRatio(zoom: Float) {
        _zoomRatio.value = zoom
    }

    fun onBarcodeDetected(rawValue: String, formatName: String) {
        val now = System.currentTimeMillis()
        // Debounce same code within 2 seconds
        if (rawValue == lastScannedRaw && now - lastScannedTimestamp < 2000) {
            return
        }

        lastScannedRaw = rawValue
        lastScannedTimestamp = now

        val parsed = BarcodeParser.parse(rawValue, formatName)
        _activeResult.value = parsed

        // Save scan to Room history
        viewModelScope.launch {
            historyRepository.addEntry(
                toolId = "qr_scanner",
                type = parsed.type.name,
                title = parsed.title,
                subtitle = parsed.subtitle,
                payloadJson = "{\"rawValue\":\"${escapeJson(rawValue)}\",\"format\":\"$formatName\"}"
            )
        }
    }

    fun scanGalleryImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    _errorMessage.value = "Failed to load image"
                    return@launch
                }

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val first = barcodes.firstOrNull()
                        if (first != null && !first.rawValue.isNullOrBlank()) {
                            val formatName = first.format.toString()
                            onBarcodeDetected(first.rawValue!!, formatName)
                        } else {
                            _errorMessage.value = "No QR code or barcode found in this image"
                        }
                    }
                    .addOnFailureListener {
                        _errorMessage.value = "Scanning failed: ${it.localizedMessage}"
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Could not open image: ${e.localizedMessage}"
            }
        }
    }

    fun clearActiveResult() {
        _activeResult.value = null
    }

    fun openHistorySheet() {
        _showHistorySheet.value = true
    }

    fun closeHistorySheet() {
        _showHistorySheet.value = false
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteEntry(id)
        }
    }

    fun toggleHistoryFavorite(id: Long) {
        viewModelScope.launch {
            historyRepository.toggleFavorite(id)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun canHandleIntent(intent: Intent): Boolean {
        return capabilities.canHandle(intent)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }
}
