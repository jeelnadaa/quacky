package app.quacky.core.capability

import android.content.Intent

enum class ArCoreStatus {
    SUPPORTED_AND_READY,
    SUPPORTED_NEEDS_INSTALL_OR_UPDATE,
    UNSUPPORTED,
    CHECKING
}

/**
 * Injectable, fakeable interface exposing device capabilities for hardware honesty checks.
 */
interface DeviceCapabilities {
    val hasBackCamera: Boolean
    val hasFlashUnit: Boolean
    val hasAccelerometer: Boolean
    val hasVibrator: Boolean
    val hasMagnetometer: Boolean
    val arCoreStatus: ArCoreStatus
    val isDisplayMetricsPlausible: Boolean

    fun canHandle(intent: Intent): Boolean
}
