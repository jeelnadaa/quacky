package app.quacky.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quacky.core.designsystem.theme.ButtonCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary

enum class QuackyButtonStyle {
    Primary,
    Secondary,
    Text
}

@Composable
fun QuackyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: QuackyButtonStyle = QuackyButtonStyle.Primary,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(ButtonCornerRadius)

    when (style) {
        QuackyButtonStyle.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuackyAccent,
                    contentColor = QuackyBackground,
                    disabledContainerColor = QuackyOutline,
                    disabledContentColor = QuackyTextTertiary
                ),
                content = content
            )
        }
        QuackyButtonStyle.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                border = BorderStroke(1.dp, if (enabled) QuackyOutline else QuackyOutline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = QuackyTextPrimary,
                    disabledContentColor = QuackyTextTertiary
                ),
                content = content
            )
        }
        QuackyButtonStyle.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = QuackyTextSecondary,
                    disabledContentColor = QuackyTextTertiary
                ),
                content = content
            )
        }
    }
}
