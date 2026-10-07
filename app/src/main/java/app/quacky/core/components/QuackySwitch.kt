package app.quacky.core.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextSecondary

object QuackySwitchDefaults {
    @Composable
    fun colors(): SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = QuackyBackground,
        checkedTrackColor = QuackyAccent,
        checkedBorderColor = QuackyAccent,
        uncheckedThumbColor = QuackyTextSecondary,
        uncheckedTrackColor = QuackySurface,
        uncheckedBorderColor = QuackyOutline
    )
}

@Composable
fun QuackySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = QuackySwitchDefaults.colors()
    )
}
