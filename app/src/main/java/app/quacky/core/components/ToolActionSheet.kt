package app.quacky.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolActionSheet(
    tool: ToolDefinition,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onHowToUse: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuackySurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // Header: Tool Icon, Name and Category
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = QuackyTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(tool.nameRes),
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = QuackyTextPrimary
                    )
                    Text(
                        text = stringResource(tool.category.titleRes),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = QuackyOutline, thickness = 1.dp)

            // Open Action
            ActionSheetRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                text = stringResource(R.string.action_open),
                onClick = {
                    onDismiss()
                    onOpen()
                }
            )

            // Pin / Unpin Action
            ActionSheetRow(
                icon = if (isPinned) Icons.Rounded.Pin else Icons.Rounded.PushPin,
                text = stringResource(if (isPinned) R.string.action_unpin else R.string.action_pin),
                onClick = {
                    onTogglePin()
                    onDismiss()
                }
            )

            // How to use Action
            ActionSheetRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                text = stringResource(R.string.action_how_to_use),
                onClick = {
                    onDismiss()
                    onHowToUse()
                }
            )
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = QuackyTextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = QuackyTextPrimary
        )
    }
}
