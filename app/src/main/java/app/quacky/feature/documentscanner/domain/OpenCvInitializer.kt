package app.quacky.feature.documentscanner.domain

import org.opencv.android.OpenCVLoader

/**
 * Thread-safe initializer and availability checker for OpenCV native libraries.
 */
object OpenCvInitializer {

    @Volatile
    private var isInitialized = false

    /**
     * Checks if OpenCV is loaded and attempts initialization if not already done.
     * Returns true if OpenCV native libraries are available and ready to use.
     */
    fun isAvailable(): Boolean {
        if (isInitialized) return true
        return synchronized(this) {
            if (isInitialized) return true
            try {
                @Suppress("DEPRECATION")
                if (OpenCVLoader.initLocal() || OpenCVLoader.initDebug()) {
                    isInitialized = true
                    true
                } else {
                    false
                }
            } catch (_: Throwable) {
                false
            }
        }
    }
}
