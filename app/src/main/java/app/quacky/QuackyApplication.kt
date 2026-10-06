package app.quacky

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuackyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fast, lightweight startup: no heavy background work or phone-home SDKs
    }
}
