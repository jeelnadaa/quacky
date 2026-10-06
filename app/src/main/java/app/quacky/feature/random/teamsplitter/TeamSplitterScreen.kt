package app.quacky.feature.random.teamsplitter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSplitterScreen(
    viewModel: TeamSplitterViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var newPlayerName by remember { mutableStateOf("") }

    ToolScaffold(
        tool = ToolRegistry.TEAM_SPLITTER,
        onBack = onBack,
        onHelpClick = onOpenHowToUse
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Teams Result Cards
            item {
                SectionLabel(text = "Generated Teams")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.teams.forEach { team ->
                        Surface(
                            shape = RoundedCornerShape(CardCornerRadius),
                            color = QuackySurface,
                            border = BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = team.name,
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = QuackyTextPrimary
                                    )
                                    Text(
                                        text = "Skill rating: ${team.totalSkill}",
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 12.sp,
                                        color = QuackyTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = team.members.joinToString(", ") { "${it.name} (${it.skill}★)" },
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 14.sp,
                                    color = QuackyTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Shuffle Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.split()
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reshuffle", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    QuackyButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = state.teams.joinToString("\n\n") { "${it.name}:\n" + it.members.joinToString(", ") { p -> p.name } }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Teams", text))
                            Toast.makeText(context, "Teams copied", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy Teams", fontSize = 14.sp)
                    }
                }
            }

            // Number of Teams Selector
            item {
                SectionLabel(text = "Number of Teams (${state.teamCount})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((2..6).toList()) { count ->
                        QuackyChip(
                            text = "$count Teams",
                            selected = state.teamCount == count,
                            onClick = { viewModel.setTeamCount(count) }
                        )
                    }
                }
            }

            // Add Member Input
            item {
                SectionLabel(text = "Members (${state.players.size})")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlayerName,
                        onValueChange = { newPlayerName = it },
                        placeholder = { Text(text = "Add player name", color = QuackyTextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuackyAccent,
                            unfocusedBorderColor = QuackyOutline,
                            focusedTextColor = QuackyTextPrimary,
                            unfocusedTextColor = QuackyTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addPlayer(newPlayerName)
                            newPlayerName = ""
                        }
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = QuackyAccent)
                    }
                }
            }

            // Member list with remove button
            items(state.players, key = { it.id }) { player ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = QuackySurface,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${player.name} (${player.skill}★)",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = QuackyTextPrimary
                        )
                        IconButton(
                            onClick = { viewModel.removePlayer(player.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = QuackyTextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
