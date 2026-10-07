package app.quacky

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuackyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fast, lightweight startup: ensure native CV libraries are initialized
        try {
            app.quacky.feature.documentscanner.domain.OpenCvInitializer.isAvailable()
        } catch (_: Throwable) {
            // Silently fall back if native libs fail to load
        }
    }
}
