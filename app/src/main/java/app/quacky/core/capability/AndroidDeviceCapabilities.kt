package app.quacky.core.capability

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.google.ar.core.ArCoreApk
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AndroidDeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceCapabilities {

    private val packageManager: PackageManager = context.packageManager

    override val hasBackCamera: Boolean by lazy {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.any { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } catch (e: Exception) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        }
    }

    override val hasFlashUnit: Boolean by lazy {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.cameraIdList?.any { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        } catch (e: Exception) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        }
    }

    override val hasAccelerometer: Boolean by lazy {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    override val hasVibrator: Boolean by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.hasVibrator() == true
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.hasVibrator() == true
        }
    }

    override val hasMagnetometer: Boolean by lazy {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    override val arCoreStatus: ArCoreStatus
        get() {
            return try {
                when (ArCoreApk.getInstance().checkAvailability(context)) {
                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArCoreStatus.SUPPORTED_AND_READY
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArCoreStatus.SUPPORTED_NEEDS_INSTALL_OR_UPDATE
                    ArCoreApk.Availability.UNKNOWN_CHECKING,
                    ArCoreApk.Availability.UNKNOWN_TIMED_OUT,
                    ArCoreApk.Availability.UNKNOWN_ERROR -> ArCoreStatus.CHECKING
                    ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArCoreStatus.UNSUPPORTED
                }
            } catch (e: Exception) {
                ArCoreStatus.UNSUPPORTED
            }
        }

    override val isDisplayMetricsPlausible: Boolean by lazy {
        val metrics = context.resources.displayMetrics
        val xdpi = metrics.xdpi
        val ydpi = metrics.ydpi
        if (xdpi !in 120f..700f || ydpi !in 120f..700f) {
            false
        } else {
            val maxDpi = maxOf(xdpi, ydpi)
            (abs(xdpi - ydpi) / maxDpi) <= 0.08f
        }
    }

    override fun canHandle(intent: Intent): Boolean {
        return try {
            val resolveInfo = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
