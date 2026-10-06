package app.quacky.feature.colorpicker.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.capability.DeviceCapabilities
import app.quacky.data.local.db.dao.PaletteDao
import app.quacky.data.local.db.entity.PaletteColorEntity
import app.quacky.data.local.db.entity.SavedPaletteEntity
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.colorpicker.domain.ColorFormats
import app.quacky.feature.colorpicker.domain.ColorMath
import app.quacky.feature.colorpicker.domain.NamedColors
import app.quacky.feature.colorpicker.domain.SampleSize
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.roundToInt

enum class ColorSource {
    CAMERA,
    GALLERY
}

data class ColorPin(
    val id: Int,
    val xNorm: Float,
    val yNorm: Float,
    val color: Int,
    val colorName: String,
    val hex: String
)

data class ColorPickerUiState(
    val hasCamera: Boolean = true,
    val source: ColorSource = ColorSource.CAMERA,
    val pins: List<ColorPin> = emptyList(),
    val activePinIndex: Int = 0,
    val sampleSize: SampleSize = SampleSize.AVG_3X3,
    val isFrozen: Boolean = false,
    val frozenBitmap: Bitmap? = null,
    val galleryBitmap: Bitmap? = null,
    val isDraggingReticle: Boolean = false,
    val zoomScale: Float = 1.0f,
    val showColorSheet: Boolean = false,
    val showPaletteSheet: Boolean = false,
    val contrastComparisonColor: Int = Color.WHITE,
    val savedPalettes: List<SavedPaletteEntity> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
) {
    val activePin: ColorPin?
        get() = pins.getOrNull(activePinIndex) ?: pins.firstOrNull()

    val activeColorFormats: ColorFormats?
        get() = activePin?.color?.let { ColorMath.toFormats(it) }
}

@HiltViewModel
class ColorPickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository,
    private val paletteDao: PaletteDao,
    private val capabilities: DeviceCapabilities
) : ViewModel() {

    private val hasCamera = capabilities.hasBackCamera

    private val _source = MutableStateFlow(if (hasCamera) ColorSource.CAMERA else ColorSource.GALLERY)
    private val _pins = MutableStateFlow<List<ColorPin>>(emptyList())
    private val _activePinIndex = MutableStateFlow(0)
    private val _sampleSize = MutableStateFlow(if (hasCamera) SampleSize.AVG_3X3 else SampleSize.POINT)
    private val _isFrozen = MutableStateFlow(false)
    private val _frozenBitmap = MutableStateFlow<Bitmap?>(null)
    private val _galleryBitmap = MutableStateFlow<Bitmap?>(null)
    private val _isDraggingReticle = MutableStateFlow(false)
    private val _zoomScale = MutableStateFlow(1.0f)
    private val _showColorSheet = MutableStateFlow(false)
    private val _showPaletteSheet = MutableStateFlow(false)
    private val _contrastColor = MutableStateFlow(Color.WHITE)
    private val _infoMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ColorPickerUiState> = combine(
        combine(_source, _pins, _activePinIndex, _sampleSize, _isFrozen) { s, p, idx, ss, f ->
            listOf(s, p, idx, ss, f)
        },
        combine(_frozenBitmap, _galleryBitmap, _isDraggingReticle, _zoomScale) { fb, gb, drag, z ->
            listOf(fb, gb, drag, z)
        },
        combine(_showColorSheet, _showPaletteSheet, _contrastColor, paletteDao.getAllPalettesFlow()) { sc, sp, cc, pal ->
            listOf(sc, sp, cc, pal)
        },
        _infoMessage,
        _errorMessage
    ) { group1, group2, group3, info, err ->
        @Suppress("UNCHECKED_CAST")
        ColorPickerUiState(
            hasCamera = hasCamera,
            source = group1[0] as ColorSource,
            pins = group1[1] as List<ColorPin>,
            activePinIndex = group1[2] as Int,
            sampleSize = group1[3] as SampleSize,
            isFrozen = group1[4] as Boolean,
            frozenBitmap = group2[0] as? Bitmap,
            galleryBitmap = group2[1] as? Bitmap,
            isDraggingReticle = group2[2] as Boolean,
            zoomScale = group2[3] as Float,
            showColorSheet = group3[0] as Boolean,
            showPaletteSheet = group3[1] as Boolean,
            contrastComparisonColor = group3[2] as Int,
            savedPalettes = group3[3] as List<SavedPaletteEntity>,
            infoMessage = info,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ColorPickerUiState(hasCamera = hasCamera)
    )

    init {
        viewModelScope.launch {
            preferences.recordToolUsed("color_picker")
            // Create default pin #1 at center
            _pins.value = listOf(
                createPin(id = 1, xNorm = 0.5f, yNorm = 0.5f, color = Color.DKGRAY)
            )
        }
    }

    fun setSource(source: ColorSource) {
        if (source == ColorSource.CAMERA && !hasCamera) {
            _errorMessage.value = "This device does not have a back camera"
            return
        }
        _source.value = source
        _sampleSize.value = if (source == ColorSource.CAMERA) SampleSize.AVG_3X3 else SampleSize.POINT
        if (source == ColorSource.GALLERY) {
            _isFrozen.value = false
        }
    }

    fun setSampleSize(size: SampleSize) {
        _sampleSize.value = size
        resampleActivePin()
    }

    fun setDragging(isDragging: Boolean) {
        _isDraggingReticle.value = isDragging
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(1.0f, 20.0f)
    }

    fun selectPin(index: Int) {
        if (index in _pins.value.indices) {
            _activePinIndex.value = index
        }
    }

    fun addPin() {
        val current = _pins.value.toMutableList()
        if (current.size >= 8) {
            _errorMessage.value = "Maximum of 8 pins reached"
            return
        }
        val nextId = (1..8).firstOrNull { id -> current.none { it.id == id } } ?: (current.size + 1)
        // Offset slightly from active pin
        val active = current.getOrNull(_activePinIndex.value)
        val newX = ((active?.xNorm ?: 0.5f) + 0.05f).coerceIn(0.1f, 0.9f)
        val newY = ((active?.yNorm ?: 0.5f) + 0.05f).coerceIn(0.1f, 0.9f)

        val newPin = createPin(nextId, newX, newY, active?.color ?: Color.GRAY)
        current.add(newPin)
        _pins.value = current
        _activePinIndex.value = current.size - 1
        resampleActivePin()
    }

    fun removePin(index: Int) {
        val current = _pins.value.toMutableList()
        if (current.size <= 1) {
            _errorMessage.value = "At least one pin must remain"
            return
        }
        if (index in current.indices) {
            current.removeAt(index)
            _pins.value = current
            _activePinIndex.value = (_activePinIndex.value.coerceAtMost(current.size - 1))
        }
    }

    fun updatePinPosition(xNorm: Float, yNorm: Float) {
        val current = _pins.value.toMutableList()
        val idx = _activePinIndex.value
        if (idx in current.indices) {
            val pin = current[idx]
            current[idx] = pin.copy(
                xNorm = xNorm.coerceIn(0f, 1f),
                yNorm = yNorm.coerceIn(0f, 1f)
            )
            _pins.value = current
            resampleActivePin()
        }
    }

    fun nudgeActivePin(dxPixels: Int, dyPixels: Int, bitmapWidth: Int, bitmapHeight: Int) {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return
        val current = _pins.value.toMutableList()
        val idx = _activePinIndex.value
        if (idx in current.indices) {
            val pin = current[idx]
            val curPxX = pin.xNorm * bitmapWidth
            val curPxY = pin.yNorm * bitmapHeight
            val newPxX = (curPxX + dxPixels).coerceIn(0f, bitmapWidth.toFloat())
            val newPxY = (curPxY + dyPixels).coerceIn(0f, bitmapHeight.toFloat())

            current[idx] = pin.copy(
                xNorm = newPxX / bitmapWidth,
                yNorm = newPxY / bitmapHeight
            )
            _pins.value = current
            resampleActivePin()
        }
    }

    fun sampleFromBitmap(bitmap: Bitmap) {
        val current = _pins.value.toMutableList()
        val idx = _activePinIndex.value
        if (idx in current.indices) {
            val pin = current[idx]
            val x = (pin.xNorm * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
            val y = (pin.yNorm * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
            val sampledColor = samplePixel(bitmap, x, y, _sampleSize.value)
            val (name, _) = NamedColors.findNearest(sampledColor)
            val hex = String.format("#%06X", 0xFFFFFF and sampledColor)

            current[idx] = pin.copy(color = sampledColor, colorName = name, hex = hex)
            _pins.value = current
        }
    }

    fun toggleFreeze(currentFrame: Bitmap?) {
        if (_isFrozen.value) {
            _isFrozen.value = false
            _frozenBitmap.value = null
        } else {
            if (currentFrame != null) {
                _frozenBitmap.value = currentFrame
                _isFrozen.value = true
                sampleFromBitmap(currentFrame)
            }
        }
    }

    fun setGalleryBitmap(bitmap: Bitmap) {
        _galleryBitmap.value = bitmap
        _source.value = ColorSource.GALLERY
        sampleFromBitmap(bitmap)
    }

    fun loadGalleryImageUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        setGalleryBitmap(bitmap)
                    }
                } else {
                    _errorMessage.value = "Failed to load image"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Could not open image: ${e.localizedMessage}"
            }
        }
    }

    fun extractPaletteFromActiveImage() {
        val bitmap = when (_source.value) {
            ColorSource.CAMERA -> _frozenBitmap.value
            ColorSource.GALLERY -> _galleryBitmap.value
        }
        if (bitmap == null) {
            _errorMessage.value = "Freeze a camera frame or load an image first to extract palette"
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val dominant = ColorMath.extractDominantColors(pixels, 6)
            withContext(Dispatchers.Main) {
                val newPins = dominant.mapIndexed { i, c ->
                    createPin(id = i + 1, xNorm = 0.2f + (i * 0.1f), yNorm = 0.5f, color = c)
                }
                _pins.value = newPins
                _activePinIndex.value = 0
                _infoMessage.value = "Extracted ${dominant.size} dominant colors into pins"
            }
        }
    }

    fun saveColorToHistory(label: String = "") {
        val pin = uiState.value.activePin ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = when (_source.value) {
                ColorSource.CAMERA -> _frozenBitmap.value
                ColorSource.GALLERY -> _galleryBitmap.value
            }

            var thumbBytes: ByteArray? = null
            if (bitmap != null) {
                val cropSize = 64
                val cx = (pin.xNorm * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                val cy = (pin.yNorm * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                val left = (cx - cropSize / 2).coerceIn(0, (bitmap.width - cropSize).coerceAtLeast(0))
                val top = (cy - cropSize / 2).coerceIn(0, (bitmap.height - cropSize).coerceAtLeast(0))
                val w = cropSize.coerceAtMost(bitmap.width - left)
                val h = cropSize.coerceAtMost(bitmap.height - top)
                if (w > 0 && h > 0) {
                    val crop = Bitmap.createBitmap(bitmap, left, top, w, h)
                    val stream = ByteArrayOutputStream()
                    crop.compress(Bitmap.CompressFormat.PNG, 90, stream)
                    thumbBytes = stream.toByteArray()
                }
            }

            historyRepository.addEntry(
                toolId = "color_picker",
                type = "COLOR",
                title = "${pin.hex} - ${pin.colorName}",
                subtitle = label.ifBlank { "Sampled Color" },
                payloadJson = "{\"hex\":\"${pin.hex}\",\"name\":\"${pin.colorName}\",\"colorInt\":${pin.color}}",
                thumbnailBytes = thumbBytes
            )
            withContext(Dispatchers.Main) {
                _infoMessage.value = "Saved ${pin.hex} to history"
            }
        }
    }

    fun savePalette(name: String) {
        val currentPins = _pins.value
        if (currentPins.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val paletteId = paletteDao.insertPalette(
                SavedPaletteEntity(name = name.ifBlank { "Palette (${currentPins.size} colors)" })
            )
            val colorEntities = currentPins.map { pin ->
                PaletteColorEntity(
                    paletteId = paletteId,
                    hex = pin.hex,
                    colorName = pin.colorName
                )
            }
            paletteDao.insertColors(colorEntities)
            withContext(Dispatchers.Main) {
                _infoMessage.value = "Palette saved"
                _showPaletteSheet.value = false
            }
        }
    }

    fun deletePalette(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            paletteDao.deletePalette(id)
        }
    }

    fun setContrastComparisonColor(color: Int) {
        _contrastColor.value = color
    }

    fun openColorSheet() {
        _showColorSheet.value = true
    }

    fun closeColorSheet() {
        _showColorSheet.value = false
    }

    fun openPaletteSheet() {
        _showPaletteSheet.value = true
    }

    fun closePaletteSheet() {
        _showPaletteSheet.value = false
    }

    fun clearMessages() {
        _infoMessage.value = null
        _errorMessage.value = null
    }

    private fun resampleActivePin() {
        val bitmap = when (_source.value) {
            ColorSource.CAMERA -> _frozenBitmap.value
            ColorSource.GALLERY -> _galleryBitmap.value
        }
        if (bitmap != null) {
            sampleFromBitmap(bitmap)
        }
    }

    private fun samplePixel(bitmap: Bitmap, x: Int, y: Int, sampleSize: SampleSize): Int {
        val half = sampleSize.size / 2
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
        for (dy in -half..half) {
            for (dx in -half..half) {
                val px = (x + dx).coerceIn(0, bitmap.width - 1)
                val py = (y + dy).coerceIn(0, bitmap.height - 1)
                val c = bitmap.getPixel(px, py)
                rSum += Color.red(c)
                gSum += Color.green(c)
                bSum += Color.blue(c)
                count++
            }
        }
        return Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    private fun createPin(id: Int, xNorm: Float, yNorm: Float, color: Int): ColorPin {
        val (name, _) = NamedColors.findNearest(color)
        val hex = String.format("#%06X", 0xFFFFFF and color)
        return ColorPin(id = id, xNorm = xNorm, yNorm = yNorm, color = color, colorName = name, hex = hex)
    }
}
