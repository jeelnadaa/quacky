package app.quacky.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.ConfirmDialog
import app.quacky.core.designsystem.component.EmptyState
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyDestructive
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import app.quacky.data.local.db.entity.HistoryEntryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalHistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToToolWithEntry: (ToolDefinition, HistoryEntryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isMultiSelectMode) {
                            "${state.selectedEntryIds.size} selected"
                        } else {
                            stringResource(R.string.nav_history)
                        },
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = QuackyTextPrimary
                    )
                },
                actions = {
                    if (state.isMultiSelectMode) {
                        IconButton(onClick = viewModel::deleteSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Selected",
                                tint = QuackyDestructive
                            )
                        }
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = QuackyTextPrimary
                            )
                        }
                    } else if (state.totalCount > 0) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Clear All",
                                tint = QuackyTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QuackyBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    QuackyChip(
                        text = "All",
                        selected = state.selectedFilterCategory == null && !state.onlyFavorites,
                        onClick = {
                            viewModel.setFilterCategory(null)
                            if (state.onlyFavorites) viewModel.toggleFavoritesFilter()
                        }
                    )
                }
                item {
                    QuackyChip(
                        text = "Favorites",
                        selected = state.onlyFavorites,
                        onClick = viewModel::toggleFavoritesFilter
                    )
                }
                items(ToolCategory.entries) { category ->
                    QuackyChip(
                        text = stringResource(category.titleRes),
                        selected = state.selectedFilterCategory == category,
                        onClick = { viewModel.setFilterCategory(category) }
                    )
                }
            }

            if (state.groupedEntries.isEmpty()) {
                EmptyState(
                    text = "No history entries found",
                    icon = Icons.Rounded.History,
                    modifier = Modifier.padding(top = 64.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    state.groupedEntries.forEach { (dayLabel, entries) ->
                        item(key = dayLabel) {
                            SectionLabel(text = dayLabel)
                        }
                        items(entries, key = { it.id }) { entry ->
                            HistoryItemCard(
                                entry = entry,
                                isSelected = state.selectedEntryIds.contains(entry.id),
                                isMultiSelectMode = state.isMultiSelectMode,
                                onClick = {
                                    if (state.isMultiSelectMode) {
                                        viewModel.toggleSelectEntry(entry.id)
                                    } else {
                                        val tool = ToolRegistry.getById(entry.toolId)
                                        if (tool != null) {
                                            onNavigateToToolWithEntry(tool, entry)
                                        }
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleSelectEntry(entry.id)
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearAllConfirm) {
        ConfirmDialog(
            title = "Clear History",
            message = "Are you sure you want to delete all history? This action cannot be undone.",
            confirmButtonText = "Clear All",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAll()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemCard(
    entry: HistoryEntryEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val tool = ToolRegistry.getById(entry.toolId)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(CardCornerRadius),
        color = if (isSelected) QuackySurface.copy(alpha = 0.8f) else QuackySurface,
        border = BorderStroke(1.dp, if (isSelected) QuackyAccent else QuackyOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) QuackyAccent else QuackyBackground,
                    border = BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.size(20.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = QuackyBackground,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else if (tool != null) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = QuackyTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = QuackyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.subtitle,
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (entry.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (entry.isFavorite) QuackyAccent else QuackyTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
