package app.quacky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.quacky.core.capability.RequirementChecker
import app.quacky.core.designsystem.theme.QuackyTheme
import app.quacky.core.navigation.AppNavHost
import app.quacky.data.local.preferences.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var requirementChecker: RequirementChecker

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install AndroidX SplashScreen API before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialToolId = intent?.getStringExtra("tool_id")

        setContent {
            QuackyTheme {
                AppNavHost(
                    requirementChecker = requirementChecker,
                    preferences = preferences,
                    initialToolId = initialToolId
                )
            }
        }
    }
}
