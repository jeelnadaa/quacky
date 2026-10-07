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
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.ButtonCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily

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
            val contentColor = if (enabled) QuackyBackground else QuackyTextTertiary
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuackyAccent,
                    contentColor = contentColor,
                    disabledContainerColor = QuackyOutline,
                    disabledContentColor = QuackyTextTertiary
                )
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides contentColor
                ) {
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.ui.text.TextStyle(
                            fontFamily = SatoshiFontFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                    ) {
                        content()
                    }
                }
            }
        }
        QuackyButtonStyle.Secondary -> {
            val contentColor = if (enabled) QuackyTextPrimary else QuackyTextTertiary
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                border = BorderStroke(1.dp, if (enabled) QuackyOutline else QuackyOutline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = contentColor,
                    disabledContentColor = QuackyTextTertiary
                )
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides contentColor
                ) {
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.ui.text.TextStyle(
                            fontFamily = SatoshiFontFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                    ) {
                        content()
                    }
                }
            }
        }
        QuackyButtonStyle.Text -> {
            val contentColor = if (enabled) QuackyTextSecondary else QuackyTextTertiary
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = contentColor,
                    disabledContentColor = QuackyTextTertiary
                )
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides contentColor
                ) {
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.ui.text.TextStyle(
                            fontFamily = SatoshiFontFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
