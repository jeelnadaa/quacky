package app.quacky.feature.arruler.presentation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.entity.HistoryEntryEntity
import app.quacky.data.local.preferences.AppPreferences
import app.quacky.data.repository.HistoryRepository
import app.quacky.feature.arruler.domain.ArMath
import app.quacky.feature.arruler.domain.ArPoint
import app.quacky.feature.arruler.domain.ArRulerMode
import app.quacky.feature.arruler.domain.ArSegment
import app.quacky.feature.arruler.domain.ArTrackingStatus
import app.quacky.feature.arruler.domain.ArUnit
import app.quacky.feature.arruler.domain.ScreenPoint
import app.quacky.feature.arruler.domain.Vector3
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

data class ArUiState(
    val mode: ArRulerMode = ArRulerMode.DISTANCE,
    val unit: ArUnit = ArUnit.CM,
    val points: List<ArPoint> = emptyList(),
    val segments: List<ArSegment> = emptyList(),
    val totalValueMeters: Float = 0f,
    val formattedTotal: String = "0.0 cm",
    val angleDegrees: Double? = null,
    val formattedAngle: String? = null,
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.SEARCHING_SURFACE,
    val reticleHit: Vector3? = null,
    val isAccuracyNoteDismissed: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isSaveDialogOpen: Boolean = false,
    val defaultLabel: String = "",
    val activeHistoryEntries: List<HistoryEntryEntity> = emptyList(),
    val snackbarMessage: String? = null,
    val sendToAreaVolumeValue: Double? = null
)

@HiltViewModel
class ArRulerViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.arRulerUnit.collectLatest { unitStr ->
                val unit = ArUnit.entries.firstOrNull { it.label == unitStr } ?: ArUnit.CM
                _uiState.update { state ->
                    val formatted = ArMath.formatMeasurement(state.totalValueMeters, unit)
                    state.copy(unit = unit, formattedTotal = formatted)
                }
            }
        }

        viewModelScope.launch {
            appPreferences.isArAccuracyNoteDismissed.collectLatest { dismissed ->
                _uiState.update { it.copy(isAccuracyNoteDismissed = dismissed) }
            }
        }

        viewModelScope.launch {
            historyRepository.getHistoryForTool(ToolRegistry.AR_RULER.id).collectLatest { entries ->
                _uiState.update { it.copy(activeHistoryEntries = entries) }
            }
        }
    }

    fun setMode(mode: ArRulerMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                points = emptyList(),
                segments = emptyList(),
                totalValueMeters = 0f,
                formattedTotal = ArMath.formatMeasurement(0f, it.unit),
                angleDegrees = null,
                formattedAngle = null
            )
        }
    }

    fun setUnit(unit: ArUnit) {
        viewModelScope.launch {
            appPreferences.setArRulerUnit(unit.label)
        }
    }

    fun dismissAccuracyNote() {
        viewModelScope.launch {
            appPreferences.setArAccuracyNoteDismissed(true)
        }
    }

    fun onFrameUpdated(
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        hasSurface: Boolean,
        reticleHit: Vector3?,
        viewportWidth: Int,
        viewportHeight: Int
    ) {
        _uiState.update { state ->
            val w = viewportWidth.toFloat()
            val h = viewportHeight.toFloat()

            // Re-project world coordinates of all placed points to 2D screen coordinates
            val updatedPoints = state.points.map { pt ->
                val screenPt = ArMath.projectWorldToScreen(pt.worldPosition, viewMatrix, projMatrix, w, h)
                pt.copy(screenPoint = screenPt)
            }

            // Recompute screen midpoints for floating distance pills
            val updatedSegments = state.segments.map { seg ->
                val fromScreen = updatedPoints.firstOrNull { it.id == seg.from.id }?.screenPoint
                val toScreen = updatedPoints.firstOrNull { it.id == seg.to.id }?.screenPoint
                val midX = if (fromScreen != null && toScreen != null) (fromScreen.x + toScreen.x) / 2f else 0f
                val midY = if (fromScreen != null && toScreen != null) (fromScreen.y + toScreen.y) / 2f else 0f
                seg.copy(midScreenX = midX, midScreenY = midY)
            }

            val status = if (hasSurface) ArTrackingStatus.SURFACE_FOUND else ArTrackingStatus.SEARCHING_SURFACE

            state.copy(
                points = updatedPoints,
                segments = updatedSegments,
                trackingStatus = status,
                reticleHit = reticleHit
            )
        }
    }

    fun onPointPlaced(worldPos: Vector3, screenX: Float, screenY: Float) {
        val currentState = _uiState.value
        val screenPt = ScreenPoint(screenX, screenY)
        val newPoint = ArPoint(
            id = UUID.randomUUID().toString(),
            worldPosition = worldPos,
            screenPoint = screenPt,
            isFloorAnchor = (currentState.mode == ArRulerMode.HEIGHT && currentState.points.isEmpty())
        )

        when (currentState.mode) {
            ArRulerMode.DISTANCE -> {
                val currentPts = currentState.points
                if (currentPts.size >= 2) {
                    // Start new measurement
                    _uiState.update {
                        it.copy(
                            points = listOf(newPoint),
                            segments = emptyList(),
                            totalValueMeters = 0f,
                            formattedTotal = ArMath.formatMeasurement(0f, it.unit)
                        )
                    }
                } else {
                    val newPts = currentPts + newPoint
                    var segs = emptyList<ArSegment>()
                    var totalMeters = 0f
                    if (newPts.size == 2) {
                        val dist = ArMath.distance(newPts[0].worldPosition, newPts[1].worldPosition)
                        totalMeters = dist
                        segs = listOf(
                            ArSegment(
                                from = newPts[0],
                                to = newPts[1],
                                lengthMeters = dist,
                                formattedLength = ArMath.formatMeasurement(dist, currentState.unit),
                                midScreenX = (screenX + (newPts[0].screenPoint?.x ?: screenX)) / 2f,
                                midScreenY = (screenY + (newPts[0].screenPoint?.y ?: screenY)) / 2f
                            )
                        )
                    }
                    _uiState.update {
                        it.copy(
                            points = newPts,
                            segments = segs,
                            totalValueMeters = totalMeters,
                            formattedTotal = ArMath.formatMeasurement(totalMeters, it.unit)
                        )
                    }
                }
            }
            ArRulerMode.PATH -> {
                val newPts = currentState.points + newPoint
                val segs = mutableListOf<ArSegment>()
                var totalMeters = 0f
                for (i in 0 until newPts.size - 1) {
                    val d = ArMath.distance(newPts[i].worldPosition, newPts[i + 1].worldPosition)
                    totalMeters += d
                    segs.add(
                        ArSegment(
                            from = newPts[i],
                            to = newPts[i + 1],
                            lengthMeters = d,
                            formattedLength = ArMath.formatMeasurement(d, currentState.unit)
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        points = newPts,
                        segments = segs,
                        totalValueMeters = totalMeters,
                        formattedTotal = ArMath.formatMeasurement(totalMeters, it.unit)
                    )
                }
            }
            ArRulerMode.HEIGHT -> {
                val currentPts = currentState.points
                if (currentPts.size >= 2) {
                    _uiState.update {
                        it.copy(
                            points = listOf(newPoint.copy(isFloorAnchor = true)),
                            segments = emptyList(),
                            totalValueMeters = 0f,
                            formattedTotal = ArMath.formatMeasurement(0f, it.unit)
                        )
                    }
                } else {
                    val newPts = currentPts + newPoint
                    var segs = emptyList<ArSegment>()
                    var heightMeters = 0f
                    if (newPts.size == 2) {
                        heightMeters = ArMath.heightDistance(newPts[0].worldPosition, newPts[1].worldPosition)
                        segs = listOf(
                            ArSegment(
                                from = newPts[0],
                                to = newPts[1],
                                lengthMeters = heightMeters,
                                formattedLength = ArMath.formatMeasurement(heightMeters, currentState.unit)
                            )
                        )
                    }
                    _uiState.update {
                        it.copy(
                            points = newPts,
                            segments = segs,
                            totalValueMeters = heightMeters,
                            formattedTotal = ArMath.formatMeasurement(heightMeters, it.unit)
                        )
                    }
                }
            }
            ArRulerMode.ANGLE -> {
                val currentPts = currentState.points
                if (currentPts.size >= 3) {
                    _uiState.update {
                        it.copy(
                            points = listOf(newPoint),
                            segments = emptyList(),
                            angleDegrees = null,
                            formattedAngle = null
                        )
                    }
                } else {
                    val newPts = currentPts + newPoint
                    var angleDeg: Double? = null
                    var formattedAng: String? = null
                    val segs = mutableListOf<ArSegment>()

                    if (newPts.size >= 2) {
                        // Ray from vertex to p1 or p2
                        segs.add(
                            ArSegment(
                                from = newPts[0],
                                to = newPts[1],
                                lengthMeters = ArMath.distance(newPts[0].worldPosition, newPts[1].worldPosition),
                                formattedLength = ""
                            )
                        )
                    }
                    if (newPts.size == 3) {
                        segs.add(
                            ArSegment(
                                from = newPts[1],
                                to = newPts[2],
                                lengthMeters = ArMath.distance(newPts[1].worldPosition, newPts[2].worldPosition),
                                formattedLength = ""
                            )
                        )
                        // In 3 points: index 1 is the vertex
                        val angle = ArMath.angleDegrees(newPts[0].worldPosition, newPts[1].worldPosition, newPts[2].worldPosition)
                        angleDeg = angle
                        formattedAng = ArMath.formatAngle(angle)
                    }

                    _uiState.update {
                        it.copy(
                            points = newPts,
                            segments = segs,
                            angleDegrees = angleDeg,
                            formattedAngle = formattedAng
                        )
                    }
                }
            }
        }
    }

    fun undoLastPoint() {
        _uiState.update { state ->
            if (state.points.isEmpty()) return@update state
            val newPts = state.points.dropLast(1)
            recalculateStateWithPoints(state, newPts)
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                points = emptyList(),
                segments = emptyList(),
                totalValueMeters = 0f,
                formattedTotal = ArMath.formatMeasurement(0f, it.unit),
                angleDegrees = null,
                formattedAngle = null
            )
        }
    }

    private fun recalculateStateWithPoints(state: ArUiState, newPts: List<ArPoint>): ArUiState {
        val segs = mutableListOf<ArSegment>()
        var totalMeters = 0f
        var angleDeg: Double? = null
        var formattedAng: String? = null

        when (state.mode) {
            ArRulerMode.DISTANCE, ArRulerMode.PATH -> {
                for (i in 0 until newPts.size - 1) {
                    val d = ArMath.distance(newPts[i].worldPosition, newPts[i + 1].worldPosition)
                    totalMeters += d
                    segs.add(
                        ArSegment(
                            from = newPts[i],
                            to = newPts[i + 1],
                            lengthMeters = d,
                            formattedLength = ArMath.formatMeasurement(d, state.unit)
                        )
                    )
                }
            }
            ArRulerMode.HEIGHT -> {
                if (newPts.size >= 2) {
                    totalMeters = ArMath.heightDistance(newPts[0].worldPosition, newPts[1].worldPosition)
                    segs.add(
                        ArSegment(
                            from = newPts[0],
                            to = newPts[1],
                            lengthMeters = totalMeters,
                            formattedLength = ArMath.formatMeasurement(totalMeters, state.unit)
                        )
                    )
                }
            }
            ArRulerMode.ANGLE -> {
                if (newPts.size >= 2) {
                    segs.add(ArSegment(from = newPts[0], to = newPts[1], lengthMeters = 0f, formattedLength = ""))
                }
                if (newPts.size >= 3) {
                    segs.add(ArSegment(from = newPts[1], to = newPts[2], lengthMeters = 0f, formattedLength = ""))
                    val angle = ArMath.angleDegrees(newPts[0].worldPosition, newPts[1].worldPosition, newPts[2].worldPosition)
                    angleDeg = angle
                    formattedAng = ArMath.formatAngle(angle)
                }
            }
        }

        return state.copy(
            points = newPts,
            segments = segs,
            totalValueMeters = totalMeters,
            formattedTotal = ArMath.formatMeasurement(totalMeters, state.unit),
            angleDegrees = angleDeg,
            formattedAngle = formattedAng
        )
    }

    fun openSaveDialog() {
        val state = _uiState.value
        val defaultName = if (state.mode == ArRulerMode.ANGLE && state.formattedAngle != null) {
            "Angle: ${state.formattedAngle}"
        } else {
            "${state.mode.name.lowercase().replaceFirstChar { it.uppercase() }}: ${state.formattedTotal}"
        }
        _uiState.update { it.copy(isSaveDialogOpen = true, defaultLabel = defaultName) }
    }

    fun closeSaveDialog() {
        _uiState.update { it.copy(isSaveDialogOpen = false) }
    }

    fun saveMeasurement(label: String, thumbnailBytes: ByteArray? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val displayValue = if (state.mode == ArRulerMode.ANGLE) state.formattedAngle ?: "0.0°" else state.formattedTotal
            val title = label.ifBlank {
                "${state.mode.name.lowercase().replaceFirstChar { it.uppercase() }}: $displayValue"
            }
            val subtitle = "$displayValue (${state.mode.name})"

            val payload = JSONObject().apply {
                put("mode", state.mode.name)
                put("valueMeters", state.totalValueMeters.toDouble())
                put("formattedValue", displayValue)
                put("unit", state.unit.label)
                if (state.angleDegrees != null) {
                    put("angleDegrees", state.angleDegrees)
                }
                put("timestamp", System.currentTimeMillis())
            }.toString()

            historyRepository.addEntry(
                toolId = ToolRegistry.AR_RULER.id,
                type = "measurement",
                title = title,
                subtitle = subtitle,
                payloadJson = payload,
                thumbnailBytes = thumbnailBytes
            )

            _uiState.update {
                it.copy(
                    isSaveDialogOpen = false,
                    snackbarMessage = "Measurement saved to history"
                )
            }
        }
    }

    fun captureScreenshot(bitmap: Bitmap, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val displayName = "AR_Measurement_${System.currentTimeMillis()}.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Quacky")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                }

                // Create thumbnail bytes for Room history
                val thumbStream = ByteArrayOutputStream()
                val thumbBitmap = Bitmap.createScaledBitmap(bitmap, 240, 320, true)
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbStream)
                val thumbBytes = thumbStream.toByteArray()

                withContext(Dispatchers.Main) {
                    saveMeasurement(
                        label = "",
                        thumbnailBytes = thumbBytes
                    )
                    _uiState.update { it.copy(snackbarMessage = "Screenshot saved to gallery") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Failed to save screenshot: ${e.message}") }
                }
            }
        }
    }

    fun toggleHistory() {
        _uiState.update { it.copy(isHistoryOpen = !it.isHistoryOpen) }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteEntry(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            val ids = _uiState.value.activeHistoryEntries.map { it.id }
            if (ids.isNotEmpty()) {
                historyRepository.deleteEntries(ids)
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun prepareSendToAreaVolume() {
        val state = _uiState.value
        if (state.totalValueMeters > 0f) {
            _uiState.update { it.copy(sendToAreaVolumeValue = state.totalValueMeters.toDouble()) }
        }
    }

    fun clearSendToAreaVolume() {
        _uiState.update { it.copy(sendToAreaVolumeValue = null) }
    }
}
