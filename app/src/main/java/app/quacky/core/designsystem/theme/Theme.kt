package app.quacky.core.designsystem.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = QuackyAccent,
    onPrimary = QuackyBackground,
    primaryContainer = QuackySurfaceElevated,
    onPrimaryContainer = QuackyTextPrimary,
    secondary = QuackyTextSecondary,
    onSecondary = QuackyBackground,
    background = QuackyBackground,
    onBackground = QuackyTextPrimary,
    surface = QuackySurface,
    onSurface = QuackyTextPrimary,
    surfaceVariant = QuackySurfaceElevated,
    onSurfaceVariant = QuackyTextSecondary,
    outline = QuackyOutline,
    outlineVariant = QuackyOutline,
    error = QuackyDestructive,
    onError = QuackyAccent
)

@Composable
fun QuackyTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = QuackyBackground.toArgb()
                window.navigationBarColor = QuackyBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                // Light status/nav bar = false means white icons on dark background
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = QuackyTypography,
        shapes = QuackyShapes,
        content = content
    )
}
