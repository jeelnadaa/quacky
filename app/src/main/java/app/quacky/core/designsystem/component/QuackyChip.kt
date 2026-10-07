package app.quacky.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.PillCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary

@Composable
fun QuackyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = app.quacky.core.haptics.rememberQuackyHaptics()
    val shape = RoundedCornerShape(PillCornerRadius)
    Surface(
        modifier = modifier.clickable {
            haptics.selection()
            onClick()
        },
        shape = shape,
        color = if (selected) QuackyAccent else QuackySurface,
        border = if (selected) null else BorderStroke(1.dp, QuackyOutline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) QuackyBackground else QuackyTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
