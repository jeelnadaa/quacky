package app.quacky.feature.arruler.engine

import android.util.Log
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import java.util.EnumSet

data class SessionConfigResult(
    val isDepthSupported: Boolean,
    val selectedCameraConfig: CameraConfig?,
    val note: String?
)

object ArSessionConfigurator {
    private const val TAG = "ArSessionConfigurator"

    fun configureSession(session: Session): SessionConfigResult {
        var isDepthSupported = false
        var note: String? = null

        // 1. Select CameraConfig (target 30 FPS, hardware depth preferred, or largest CPU image resolution)
        var selectedConfig: CameraConfig? = null
        try {
            val depthFilter = CameraConfigFilter(session).apply {
                setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.REQUIRE_AND_USE))
            }
            var supportedConfigs = session.getSupportedCameraConfigs(depthFilter)

            if (supportedConfigs.isEmpty()) {
                val fallbackFilter = CameraConfigFilter(session).apply {
                    setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                }
                supportedConfigs = session.getSupportedCameraConfigs(fallbackFilter)
            }

            selectedConfig = supportedConfigs.maxByOrNull {
                it.imageSize.width * it.imageSize.height
            } ?: supportedConfigs.firstOrNull()

            if (selectedConfig != null) {
                session.cameraConfig = selectedConfig
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure custom camera config", e)
        }

        // 2. Configure Session settings
        val config = session.config.apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            focusMode = Config.FocusMode.AUTO

            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                depthMode = Config.DepthMode.AUTOMATIC
                isDepthSupported = true
            } else {
                depthMode = Config.DepthMode.DISABLED
                isDepthSupported = false
                note = "This phone has no depth sensing, so measurements on curved or small objects are less reliable."
            }

            instantPlacementMode = Config.InstantPlacementMode.DISABLED
            lightEstimationMode = Config.LightEstimationMode.DISABLED
            cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        }

        session.configure(config)

        return SessionConfigResult(
            isDepthSupported = isDepthSupported,
            selectedCameraConfig = selectedConfig,
            note = note
        )
    }
}
