package app.quacky.feature.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.brand.QuackyMark
import app.quacky.core.designsystem.component.ConfirmDialog
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAllGuides: () -> Unit,
    onNavigateToEasterEgg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var easterEggTapCount by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = QuackyTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QuackyBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsSwitchRow(
                        title = "Haptic feedback",
                        subtitle = "Vibrate on key actions and gestures",
                        checked = state.isHapticsEnabled,
                        onCheckedChange = viewModel::setHapticsEnabled
                    )
                }
            }

            // 2. Home Screen Section
            item {
                SettingsSection(title = "Home Screen") {
                    SettingsSwitchRow(
                        title = "Flat grid on Home",
                        subtitle = "Show all tools in a single list instead of categories",
                        checked = state.isFlatGrid,
                        onCheckedChange = viewModel::setFlatGrid
                    )
                    HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                    SettingsSwitchRow(
                        title = "Show Recent tools",
                        subtitle = "Display up to 4 recently used tools on Home",
                        checked = state.isShowRecents,
                        onCheckedChange = viewModel::setShowRecents
                    )
                }
            }

            // 3. History Section
            item {
                SettingsSection(title = "History") {
                    SettingsClickableRow(
                        title = "Clear all history",
                        subtitle = "Delete all stored items across all tools",
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // 4. Help & Tips Section
            item {
                SettingsSection(title = "Help & Tips") {
                    SettingsSwitchRow(
                        title = "Show tips on first open",
                        subtitle = "Display interactive guide when opening a tool for the first time",
                        checked = state.isTipsOnFirstOpen,
                        onCheckedChange = viewModel::setTipsOnFirstOpen
                    )
                    HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                    SettingsClickableRow(
                        title = "Reset all tips",
                        subtitle = "Allow first-open guides to appear again",
                        onClick = {
                            viewModel.resetAllTips()
                            Toast.makeText(context, "All tips reset", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                    SettingsClickableRow(
                        title = "All how-to guides",
                        subtitle = "Browse guides for every tool",
                        onClick = onNavigateToAllGuides
                    )
                }
            }

            // 5. About Section
            item {
                SettingsSection(title = "About") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        QuackyMark(
                            size = 48.dp,
                            tint = QuackyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QuackyTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Hidden Easter Egg Trigger: 7 taps on version number
                        Text(
                            text = "Version 1.0.0",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            color = QuackyTextSecondary,
                            modifier = Modifier
                                .clickable {
                                    easterEggTapCount++
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val remaining = 7 - easterEggTapCount
                                    if (remaining in 1..4) {
                                        Toast.makeText(context, "$remaining more quacks…", Toast.LENGTH_SHORT).show()
                                    } else if (remaining <= 0) {
                                        easterEggTapCount = 0
                                        viewModel.onEasterEggUnlocked()
                                        onNavigateToEasterEgg()
                                    }
                                }
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_tagline),
                            fontFamily = SatoshiFontFamily,
                            fontSize = 12.sp,
                            color = QuackyTextTertiary
                        )
                    }

                    if (state.isEasterEggFound) {
                        HorizontalDivider(color = QuackyOutline, thickness = 1.dp)
                        SettingsClickableRow(
                            title = stringResource(R.string.easter_egg_developer_note_row),
                            subtitle = "Read the letter from the creator",
                            onClick = onNavigateToEasterEgg
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showClearHistoryDialog) {
        ConfirmDialog(
            title = "Clear All History",
            message = "This will permanently delete all history entries across all tools.",
            confirmButtonText = "Clear All",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllHistory()
                showClearHistoryDialog = false
                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        SectionLabel(text = title)
        Surface(
            shape = RoundedCornerShape(CardCornerRadius),
            color = QuackySurface,
            border = BorderStroke(1.dp, QuackyOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                color = QuackyTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = QuackyAccent,
                checkedTrackColor = QuackySurface,
                uncheckedThumbColor = QuackyTextTertiary,
                uncheckedTrackColor = QuackyBackground
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = QuackyTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                color = QuackyTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = QuackyTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
