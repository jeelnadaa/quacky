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
import app.quacky.feature.arruler.domain.ArSessionDetail
import app.quacky.feature.arruler.domain.ArSingleMeasurement
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

data class ArUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionName: String = "AR Measurement Session",
    val mode: ArRulerMode = ArRulerMode.DISTANCE,
    val unit: ArUnit = ArUnit.CM,
    val autoFinishEnabled: Boolean = true,
    val activeMeasurement: ArSingleMeasurement? = null,
    val finishedMeasurements: List<ArSingleMeasurement> = emptyList(),
    val selectedMeasurementId: String? = null,
    val managingMeasurementId: String? = null,
    val trackingStatus: ArTrackingStatus = ArTrackingStatus.SEARCHING_SURFACE,
    val reticleHit: Vector3? = null,
    val reticleDistanceMeters: Float? = null,
    val surfaceFeaturePoints: List<ScreenPoint> = emptyList(),
    val isAccuracyNoteDismissed: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isSessionListOpen: Boolean = false,
    val isSessionLimitReached: Boolean = false,
    val activeHistoryEntries: List<HistoryEntryEntity> = emptyList(),
    val viewingSessionDetail: ArSessionDetail? = null,
    val snackbarMessage: String? = null,
    val sendToAreaVolumeValue: Double? = null
) {
    val totalMeasurementsCount: Int
        get() = finishedMeasurements.size + (if (activeMeasurement != null) 1 else 0)

    val canAddNew: Boolean
        get() = finishedMeasurements.size < 20
}

@HiltViewModel
class ArRulerViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    init {
        // Start initial empty measurement M1
        startNewMeasurement(ArRulerMode.DISTANCE)

        viewModelScope.launch {
            appPreferences.arRulerUnit.collectLatest { unitStr ->
                val unit = ArUnit.entries.firstOrNull { it.label == unitStr } ?: ArUnit.CM
                _uiState.update { state ->
                    val updatedFinished = state.finishedMeasurements.map { m ->
                        val formatted = ArMath.formatMeasurement(m.totalValueMeters, unit)
                        m.copy(unit = unit, formattedValue = formatted)
                    }
                    val updatedActive = state.activeMeasurement?.let { act ->
                        val formatted = ArMath.formatMeasurement(act.totalValueMeters, unit)
                        act.copy(unit = unit, formattedValue = formatted)
                    }
                    state.copy(
                        unit = unit,
                        finishedMeasurements = updatedFinished,
                        activeMeasurement = updatedActive
                    )
                }
            }
        }

        viewModelScope.launch {
            appPreferences.isArAutoFinishEnabled.collectLatest { autoFinish ->
                _uiState.update { it.copy(autoFinishEnabled = autoFinish) }
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
        _uiState.update { state ->
            val active = state.activeMeasurement
            if (active != null) {
                // Change mode of active measurement and reset its points
                val updated = active.copy(
                    mode = mode,
                    points = emptyList(),
                    segments = emptyList(),
                    totalValueMeters = 0f,
                    formattedValue = ArMath.formatMeasurement(0f, state.unit),
                    angleDegrees = null,
                    formattedAngle = null
                )
                state.copy(mode = mode, activeMeasurement = updated)
            } else {
                state.copy(mode = mode)
            }
        }
    }

    fun setUnit(unit: ArUnit) {
        viewModelScope.launch {
            appPreferences.setArRulerUnit(unit.label)
        }
    }

    fun setAutoFinish(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setArAutoFinishEnabled(enabled)
        }
    }

    fun dismissAccuracyNote() {
        viewModelScope.launch {
            appPreferences.setArAccuracyNoteDismissed(true)
        }
    }

    fun startNewMeasurement(requestedMode: ArRulerMode? = null) {
        val state = _uiState.value
        if (state.finishedMeasurements.size >= 20) {
            _uiState.update { it.copy(isSessionLimitReached = true) }
            return
        }

        // If active measurement already has points, lock it first
        if (state.activeMeasurement != null && state.activeMeasurement.points.isNotEmpty()) {
            finishActiveMeasurement()
        }

        val nextIndex = state.finishedMeasurements.size + 1
        val mode = requestedMode ?: state.mode
        val newMeasurement = ArSingleMeasurement(
            id = UUID.randomUUID().toString(),
            index = nextIndex,
            name = "M$nextIndex",
            mode = mode,
            unit = state.unit,
            points = emptyList(),
            segments = emptyList(),
            totalValueMeters = 0f,
            formattedValue = ArMath.formatMeasurement(0f, state.unit),
            colorToneIndex = (nextIndex - 1) % 5
        )

        _uiState.update {
            it.copy(
                activeMeasurement = newMeasurement,
                mode = mode,
                selectedMeasurementId = newMeasurement.id
            )
        }
    }

    fun finishActiveMeasurement() {
        val state = _uiState.value
        val active = state.activeMeasurement ?: return
        if (active.points.isEmpty()) return

        val finished = active.copy(isFinished = true)
        val updatedFinished = state.finishedMeasurements + finished

        _uiState.update {
            it.copy(
                activeMeasurement = null,
                finishedMeasurements = updatedFinished,
                selectedMeasurementId = finished.id,
                snackbarMessage = "${finished.name} finished: ${finished.displayValue}"
            )
        }

        // Auto-save discrete measurement and update session in history
        autoSaveSessionHistory()
    }

    fun selectMeasurement(id: String?) {
        _uiState.update { it.copy(selectedMeasurementId = id) }
    }

    fun openManageSheet(id: String? = null) {
        _uiState.update { it.copy(isSessionListOpen = true, managingMeasurementId = id) }
    }

    fun closeSessionList() {
        _uiState.update { it.copy(isSessionListOpen = false, managingMeasurementId = null) }
    }

    fun dismissLimitDialog() {
        _uiState.update { it.copy(isSessionLimitReached = false) }
    }

    fun onPointPlaced(worldPos: Vector3, screenX: Float, screenY: Float) {
        val state = _uiState.value
        if (state.finishedMeasurements.size >= 20 && state.activeMeasurement == null) {
            _uiState.update { it.copy(isSessionLimitReached = true) }
            return
        }

        // If no active measurement, start one automatically on tap!
        var active = state.activeMeasurement
        if (active == null) {
            val nextIndex = state.finishedMeasurements.size + 1
            active = ArSingleMeasurement(
                id = UUID.randomUUID().toString(),
                index = nextIndex,
                name = "M$nextIndex",
                mode = state.mode,
                unit = state.unit,
                colorToneIndex = (nextIndex - 1) % 5
            )
        }

        val screenPt = ScreenPoint(screenX, screenY)
        val newPoint = ArPoint(
            id = UUID.randomUUID().toString(),
            worldPosition = worldPos,
            screenPoint = screenPt,
            isFloorAnchor = (active.mode == ArRulerMode.HEIGHT && active.points.isEmpty())
        )

        val updatedPoints = active.points + newPoint
        val recalculated = recalculateSingleMeasurement(active.copy(points = updatedPoints))

        // Check auto-finish conditions
        var shouldAutoFinish = false
        if (state.autoFinishEnabled) {
            when (recalculated.mode) {
                ArRulerMode.DISTANCE -> if (recalculated.points.size >= 2) shouldAutoFinish = true
                ArRulerMode.HEIGHT -> if (recalculated.points.size >= 2) shouldAutoFinish = true
                ArRulerMode.ANGLE -> if (recalculated.points.size >= 3) shouldAutoFinish = true
                ArRulerMode.PATH -> shouldAutoFinish = false // Path can take indefinite points until tick is pressed
            }
        }

        if (shouldAutoFinish) {
            val finished = recalculated.copy(isFinished = true)
            val updatedFinishedList = state.finishedMeasurements + finished
            _uiState.update {
                it.copy(
                    activeMeasurement = null,
                    finishedMeasurements = updatedFinishedList,
                    selectedMeasurementId = finished.id,
                    snackbarMessage = "${finished.name} finished: ${finished.displayValue}"
                )
            }
            autoSaveSessionHistory()
        } else {
            _uiState.update {
                it.copy(
                    activeMeasurement = recalculated,
                    selectedMeasurementId = recalculated.id
                )
            }
        }
    }

    fun onFrameUpdated(
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        hasSurface: Boolean,
        reticleHit: Vector3?,
        featurePoints: List<Vector3> = emptyList(),
        viewportWidth: Int,
        viewportHeight: Int
    ) {
        val w = viewportWidth.toFloat()
        val h = viewportHeight.toFloat()
        val status = if (hasSurface) ArTrackingStatus.SURFACE_FOUND else ArTrackingStatus.SEARCHING_SURFACE

        val projectedFeatures = featurePoints.mapNotNull { pt ->
            ArMath.projectWorldToScreen(pt, viewMatrix, projMatrix, w, h)
        }

        val reticleDist = reticleHit?.let {
            kotlin.math.hypot(it.x, kotlin.math.hypot(it.y, it.z))
        }

        _uiState.update { state ->
            // Update projections for active measurement
            val updatedActive = state.activeMeasurement?.let { act ->
                updateMeasurementProjections(act, viewMatrix, projMatrix, w, h)
            }

            // Update projections for all finished measurements
            val updatedFinished = state.finishedMeasurements.map { m ->
                updateMeasurementProjections(m, viewMatrix, projMatrix, w, h)
            }

            state.copy(
                activeMeasurement = updatedActive,
                finishedMeasurements = updatedFinished,
                trackingStatus = status,
                reticleHit = reticleHit,
                reticleDistanceMeters = reticleDist,
                surfaceFeaturePoints = projectedFeatures
            )
        }
    }

    private fun updateMeasurementProjections(
        measurement: ArSingleMeasurement,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        w: Float,
        h: Float
    ): ArSingleMeasurement {
        val updatedPoints = measurement.points.map { pt ->
            val screenPt = ArMath.projectWorldToScreen(pt.worldPosition, viewMatrix, projMatrix, w, h)
            pt.copy(screenPoint = screenPt)
        }

        val updatedSegments = measurement.segments.map { seg ->
            val fromScreen = updatedPoints.firstOrNull { it.id == seg.from.id }?.screenPoint
            val toScreen = updatedPoints.firstOrNull { it.id == seg.to.id }?.screenPoint
            val midX = if (fromScreen != null && toScreen != null) (fromScreen.x + toScreen.x) / 2f else 0f
            val midY = if (fromScreen != null && toScreen != null) (fromScreen.y + toScreen.y) / 2f else 0f
            seg.copy(midScreenX = midX, midScreenY = midY)
        }

        return measurement.copy(points = updatedPoints, segments = updatedSegments)
    }

    private fun recalculateSingleMeasurement(measurement: ArSingleMeasurement): ArSingleMeasurement {
        val pts = measurement.points
        val segs = mutableListOf<ArSegment>()
        var totalMeters = 0f
        var angleDeg: Double? = null
        var formattedAng: String? = null

        when (measurement.mode) {
            ArRulerMode.DISTANCE -> {
                if (pts.size >= 2) {
                    val d = ArMath.distance(pts[0].worldPosition, pts[1].worldPosition)
                    totalMeters = d
                    segs.add(
                        ArSegment(
                            from = pts[0],
                            to = pts[1],
                            lengthMeters = d,
                            formattedLength = ArMath.formatMeasurement(d, measurement.unit)
                        )
                    )
                }
            }
            ArRulerMode.PATH -> {
                for (i in 0 until pts.size - 1) {
                    val d = ArMath.distance(pts[i].worldPosition, pts[i + 1].worldPosition)
                    totalMeters += d
                    segs.add(
                        ArSegment(
                            from = pts[i],
                            to = pts[i + 1],
                            lengthMeters = d,
                            formattedLength = ArMath.formatMeasurement(d, measurement.unit)
                        )
                    )
                }
            }
            ArRulerMode.HEIGHT -> {
                if (pts.size >= 2) {
                    totalMeters = ArMath.heightDistance(pts[0].worldPosition, pts[1].worldPosition)
                    segs.add(
                        ArSegment(
                            from = pts[0],
                            to = pts[1],
                            lengthMeters = totalMeters,
                            formattedLength = ArMath.formatMeasurement(totalMeters, measurement.unit)
                        )
                    )
                }
            }
            ArRulerMode.ANGLE -> {
                if (pts.size >= 2) {
                    segs.add(ArSegment(from = pts[0], to = pts[1], lengthMeters = 0f, formattedLength = ""))
                }
                if (pts.size >= 3) {
                    segs.add(ArSegment(from = pts[1], to = pts[2], lengthMeters = 0f, formattedLength = ""))
                    val angle = ArMath.angleDegrees(pts[0].worldPosition, pts[1].worldPosition, pts[2].worldPosition)
                    angleDeg = angle
                    formattedAng = ArMath.formatAngle(angle)
                }
            }
        }

        return measurement.copy(
            segments = segs,
            totalValueMeters = totalMeters,
            formattedValue = ArMath.formatMeasurement(totalMeters, measurement.unit),
            angleDegrees = angleDeg,
            formattedAngle = formattedAng
        )
    }

    fun undoLastPoint() {
        _uiState.update { state ->
            val active = state.activeMeasurement
            if (active != null && active.points.isNotEmpty()) {
                val newPts = active.points.dropLast(1)
                val recalculated = recalculateSingleMeasurement(active.copy(points = newPts))
                state.copy(activeMeasurement = recalculated)
            } else if (state.finishedMeasurements.isNotEmpty()) {
                // If no active points, reopen last finished measurement for editing
                val last = state.finishedMeasurements.last()
                val remaining = state.finishedMeasurements.dropLast(1)
                state.copy(
                    activeMeasurement = last.copy(isFinished = false),
                    finishedMeasurements = remaining,
                    selectedMeasurementId = last.id
                )
            } else {
                state
            }
        }
    }

    fun renameMeasurement(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            val updated = state.finishedMeasurements.map {
                if (it.id == id) it.copy(name = trimmed) else it
            }
            state.copy(finishedMeasurements = updated)
        }
        autoSaveSessionHistory()
    }

    fun changeMeasurementUnit(id: String, newUnit: ArUnit) {
        _uiState.update { state ->
            val updated = state.finishedMeasurements.map { m ->
                if (m.id == id) {
                    val formatted = ArMath.formatMeasurement(m.totalValueMeters, newUnit)
                    m.copy(unit = newUnit, formattedValue = formatted)
                } else m
            }
            state.copy(finishedMeasurements = updated)
        }
        autoSaveSessionHistory()
    }

    fun toggleFavorite(id: String) {
        _uiState.update { state ->
            val updated = state.finishedMeasurements.map {
                if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
            }
            state.copy(finishedMeasurements = updated)
        }
        autoSaveSessionHistory()
    }

    fun duplicateMeasurement(id: String) {
        val state = _uiState.value
        if (state.finishedMeasurements.size >= 20) {
            _uiState.update { it.copy(isSessionLimitReached = true) }
            return
        }
        val target = state.finishedMeasurements.firstOrNull { it.id == id } ?: return
        val nextIndex = state.finishedMeasurements.size + 1
        val duplicated = target.copy(
            id = UUID.randomUUID().toString(),
            index = nextIndex,
            name = "${target.name} (Copy)",
            colorToneIndex = (nextIndex - 1) % 5
        )
        _uiState.update {
            it.copy(
                finishedMeasurements = it.finishedMeasurements + duplicated,
                selectedMeasurementId = duplicated.id,
                snackbarMessage = "Duplicated ${target.name}"
            )
        }
        autoSaveSessionHistory()
    }

    fun deleteMeasurement(id: String) {
        _uiState.update { state ->
            val updated = state.finishedMeasurements.filterNot { it.id == id }
            state.copy(
                finishedMeasurements = updated,
                selectedMeasurementId = if (state.selectedMeasurementId == id) updated.firstOrNull()?.id else state.selectedMeasurementId,
                managingMeasurementId = null,
                snackbarMessage = "Measurement deleted"
            )
        }
        autoSaveSessionHistory()
    }

    fun deleteMultipleMeasurements(ids: Set<String>) {
        _uiState.update { state ->
            val updated = state.finishedMeasurements.filterNot { ids.contains(it.id) }
            state.copy(
                finishedMeasurements = updated,
                selectedMeasurementId = updated.firstOrNull()?.id,
                snackbarMessage = "Deleted ${ids.size} measurements"
            )
        }
        autoSaveSessionHistory()
    }

    fun editMeasurementPoints(id: String) {
        val state = _uiState.value
        val target = state.finishedMeasurements.firstOrNull { it.id == id } ?: return
        val remaining = state.finishedMeasurements.filterNot { it.id == id }
        _uiState.update {
            it.copy(
                finishedMeasurements = remaining,
                activeMeasurement = target.copy(isFinished = false),
                mode = target.mode,
                selectedMeasurementId = target.id,
                isSessionListOpen = false,
                snackbarMessage = "Editing points for ${target.name}"
            )
        }
    }

    fun copyAllMeasurementsSummary(): String {
        val state = _uiState.value
        val sb = StringBuilder()
        sb.appendLine("Session: ${state.sessionName}")
        sb.appendLine("Measurements (${state.finishedMeasurements.size}):")
        state.finishedMeasurements.forEach { m ->
            sb.appendLine("• ${m.name}: ${m.displayValue} [${m.mode.name}]")
        }
        return sb.toString()
    }

    fun clearAllMeasurements() {
        _uiState.update {
            it.copy(
                activeMeasurement = null,
                finishedMeasurements = emptyList(),
                selectedMeasurementId = null
            )
        }
        startNewMeasurement()
    }

    private fun autoSaveSessionHistory(thumbnailBytes: ByteArray? = null) {
        val state = _uiState.value
        if (state.finishedMeasurements.isEmpty()) return

        viewModelScope.launch {
            val measurementsArray = JSONArray()
            state.finishedMeasurements.forEach { m ->
                val mJson = JSONObject().apply {
                    put("id", m.id)
                    put("index", m.index)
                    put("name", m.name)
                    put("mode", m.mode.name)
                    put("unit", m.unit.label)
                    put("valueMeters", m.totalValueMeters.toDouble())
                    put("formattedValue", m.formattedValue)
                    if (m.angleDegrees != null) put("angleDegrees", m.angleDegrees)
                    put("isFavorite", m.isFavorite)
                    put("note", m.note)
                }
                measurementsArray.put(mJson)
            }

            val sessionPayload = JSONObject().apply {
                put("sessionId", state.sessionId)
                put("sessionName", state.sessionName)
                put("measurementsCount", state.finishedMeasurements.size)
                put("measurements", measurementsArray)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val title = state.sessionName
            val subtitle = "${state.finishedMeasurements.size} measurement${if (state.finishedMeasurements.size == 1) "" else "s"}"

            historyRepository.addEntry(
                toolId = ToolRegistry.AR_RULER.id,
                type = "SESSION",
                title = title,
                subtitle = subtitle,
                payloadJson = sessionPayload,
                thumbnailBytes = thumbnailBytes
            )
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

                val thumbStream = ByteArrayOutputStream()
                val thumbBitmap = Bitmap.createScaledBitmap(bitmap, 240, 320, true)
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbStream)
                val thumbBytes = thumbStream.toByteArray()

                withContext(Dispatchers.Main) {
                    autoSaveSessionHistory(thumbnailBytes = thumbBytes)
                    _uiState.update { it.copy(snackbarMessage = "Saved to Pictures/Quacky") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(snackbarMessage = "Failed to save screenshot: ${e.message}") }
                }
            }
        }
    }

    fun setSessionName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update { it.copy(sessionName = trimmed) }
            autoSaveSessionHistory()
        }
    }

    fun toggleHistory() {
        _uiState.update { it.copy(isHistoryOpen = !it.isHistoryOpen) }
    }

    fun openSessionDetail(entry: HistoryEntryEntity) {
        try {
            val json = JSONObject(entry.payloadJson)
            val sessionId = json.optString("sessionId", UUID.randomUUID().toString())
            val sessionName = json.optString("sessionName", entry.title)
            val timestamp = json.optLong("timestamp", entry.createdAt)
            val measurementsJson = json.optJSONArray("measurements") ?: JSONArray()
            val list = mutableListOf<ArSingleMeasurement>()

            for (i in 0 until measurementsJson.length()) {
                val mJson = measurementsJson.getJSONObject(i)
                val mMode = ArRulerMode.valueOf(mJson.optString("mode", ArRulerMode.DISTANCE.name))
                val mUnit = ArUnit.entries.firstOrNull { it.label == mJson.optString("unit", "cm") } ?: ArUnit.CM
                list.add(
                    ArSingleMeasurement(
                        id = mJson.optString("id", UUID.randomUUID().toString()),
                        index = mJson.optInt("index", i + 1),
                        name = mJson.optString("name", "M${i + 1}"),
                        mode = mMode,
                        unit = mUnit,
                        totalValueMeters = mJson.optDouble("valueMeters", 0.0).toFloat(),
                        formattedValue = mJson.optString("formattedValue", "0.0 cm"),
                        angleDegrees = if (mJson.has("angleDegrees")) mJson.optDouble("angleDegrees") else null,
                        isFavorite = mJson.optBoolean("isFavorite", false),
                        note = mJson.optString("note", "")
                    )
                )
            }

            val detail = ArSessionDetail(
                sessionId = sessionId,
                sessionName = sessionName,
                timestamp = timestamp,
                coverScreenshotPath = entry.thumbnailPath,
                measurements = list
            )
            _uiState.update { it.copy(viewingSessionDetail = detail) }
        } catch (_: Exception) {}
    }

    fun closeSessionDetail() {
        _uiState.update { it.copy(viewingSessionDetail = null) }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteEntry(id)
            if (_uiState.value.viewingSessionDetail != null) {
                closeSessionDetail()
            }
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

    fun sendMeasurementToAreaVolume(valueMeters: Double) {
        _uiState.update { it.copy(sendToAreaVolumeValue = valueMeters) }
    }

    fun clearSendToAreaVolume() {
        _uiState.update { it.copy(sendToAreaVolumeValue = null) }
    }
}
