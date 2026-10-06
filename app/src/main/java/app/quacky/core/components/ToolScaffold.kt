package app.quacky.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    tool: ToolDefinition,
    onBack: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    onResetClick: (() -> Unit)? = null,
    additionalActions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(tool.nameRes),
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QuackyTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = QuackyTextPrimary
                        )
                    }
                },
                actions = {
                    if (onTogglePin != null) {
                        IconButton(onClick = onTogglePin) {
                            Icon(
                                imageVector = if (isPinned) Icons.Rounded.Pin else Icons.Rounded.PushPin,
                                contentDescription = stringResource(if (isPinned) R.string.action_unpin else R.string.action_pin),
                                tint = if (isPinned) QuackyTextPrimary else QuackyTextSecondary
                            )
                        }
                    }
                    if (onHistoryClick != null) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = stringResource(R.string.nav_history),
                                tint = QuackyTextPrimary
                            )
                        }
                    }
                    if (onHelpClick != null) {
                        IconButton(onClick = onHelpClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(R.string.action_how_to_use),
                                tint = QuackyTextPrimary
                            )
                        }
                    }
                    additionalActions?.invoke(this)

                    if (onResetClick != null) {
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    tint = QuackyTextSecondary
                                )
                            }
                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = "Reset") },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onResetClick()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QuackyBackground
                )
            )
        },
        content = content
    )
}
