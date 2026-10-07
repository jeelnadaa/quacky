package app.quacky.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.brand.QuackyMark
import app.quacky.core.components.CategoryRow
import app.quacky.core.components.ToolActionSheet
import app.quacky.core.components.ToolTile
import app.quacky.core.designsystem.component.EmptyState
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.PillCornerRadius
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTool: (ToolDefinition) -> Unit,
    onNavigateToCategory: (ToolCategory) -> Unit,
    onOpenHowToUse: (ToolDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedToolForAction by remember { mutableStateOf<ToolDefinition?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // 1. Header: Greeting-free header, App name left, Quacky mascot right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = QuackyTextPrimary
                )
                QuackyMark(
                    size = 32.dp,
                    tint = QuackyTextPrimary
                )
            }

            // 2. Search Bar: Pill, surface, placeholder "Search tools"
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onClearQuery = { viewModel.onSearchQueryChanged("") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Content: If searching, show results; otherwise show Home sections
            if (state.searchQuery.isNotBlank()) {
                SearchResultsList(
                    query = state.searchQuery,
                    results = state.searchResults,
                    onToolClick = {
                        viewModel.recordToolUsed(it.id)
                        onNavigateToTool(it)
                    },
                    onToolLongClick = { selectedToolForAction = it }
                )
            } else {
                HomeSectionsList(
                    state = state,
                    onToolClick = {
                        viewModel.recordToolUsed(it.id)
                        onNavigateToTool(it)
                    },
                    onToolLongClick = { selectedToolForAction = it },
                    onCategoryClick = onNavigateToCategory
                )
            }
        }
    }

    // Action Bottom Sheet for Pinned / Long-pressed tool
    selectedToolForAction?.let { tool ->
        val isPinned = state.pinnedTools.any { it.id == tool.id }
        ToolActionSheet(
            tool = tool,
            isPinned = isPinned,
            onDismiss = { selectedToolForAction = null },
            onOpen = {
                viewModel.recordToolUsed(tool.id)
                onNavigateToTool(tool)
            },
            onTogglePin = { viewModel.togglePin(tool.id) },
            onHowToUse = { onOpenHowToUse(tool) }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(PillCornerRadius),
        color = QuackySurface,
        border = BorderStroke(1.dp, QuackyOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = QuackyTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_tools_hint),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 15.sp,
                        color = QuackyTextTertiary
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = SatoshiFontFamily,
                        fontSize = 15.sp,
                        color = QuackyTextPrimary
                    ),
                    cursorBrush = SolidColor(QuackyTextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = QuackyTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionsList(
    state: HomeUiState,
    onToolClick: (ToolDefinition) -> Unit,
    onToolLongClick: (ToolDefinition) -> Unit,
    onCategoryClick: (ToolCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 3. Pinned Section
        item {
            Column {
                SectionLabel(text = stringResource(R.string.pinned_section_title))
                if (state.pinnedTools.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pinned_empty_hint),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextTertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.pinnedTools, key = { it.id }) { tool ->
                            ToolTile(
                                tool = tool,
                                onClick = { onToolClick(tool) },
                                onLongClick = { onToolLongClick(tool) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Recent Section (last 4 tools, auto from usage log)
        if (state.isShowRecents && state.recentTools.isNotEmpty()) {
            item {
                Column {
                    SectionLabel(text = stringResource(R.string.recent_section_title))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.recentTools, key = { it.id }) { tool ->
                            ToolTile(
                                tool = tool,
                                onClick = { onToolClick(tool) },
                                onLongClick = { onToolLongClick(tool) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Categories or Flat Grid Section
        if (state.isFlatGrid) {
            item {
                SectionLabel(text = "All Tools")
            }
            items(state.allTools, key = { it.id }) { tool ->
                ToolTile(
                    tool = tool,
                    onClick = { onToolClick(tool) },
                    onLongClick = { onToolLongClick(tool) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            item {
                SectionLabel(text = stringResource(R.string.categories_section_title))
            }
            items(state.categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onClick = { onCategoryClick(category) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SearchResultsList(
    query: String,
    results: List<ToolDefinition>,
    onToolClick: (ToolDefinition) -> Unit,
    onToolLongClick: (ToolDefinition) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState(
            text = stringResource(R.string.no_tools_found, query),
            modifier = Modifier.padding(top = 48.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionLabel(text = "Search Results (${results.size})")
            }
            items(results, key = { it.id }) { tool ->
                ToolTile(
                    tool = tool,
                    onClick = { onToolClick(tool) },
                    onLongClick = { onToolLongClick(tool) }
                )
            }
        }
    }
}
