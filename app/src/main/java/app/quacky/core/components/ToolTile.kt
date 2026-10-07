package app.quacky.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolDefinition

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolTile(
    tool: ToolDefinition,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = app.quacky.core.haptics.rememberQuackyHaptics()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptics.click()
                    onClick()
                },
                onLongClick = {
                    haptics.heavy()
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(CardCornerRadius),
        color = QuackySurface,
        border = BorderStroke(1.dp, QuackyOutline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = QuackyTextPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = stringResource(tool.nameRes),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = QuackyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(tool.category.titleRes),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = QuackyTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
