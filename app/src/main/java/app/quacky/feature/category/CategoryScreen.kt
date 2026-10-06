package app.quacky.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolActionSheet
import app.quacky.core.components.ToolTile
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: ToolCategory,
    onBack: () -> Unit,
    onNavigateToTool: (ToolDefinition) -> Unit,
    onOpenHowToUse: (ToolDefinition) -> Unit,
    isToolPinned: (String) -> Boolean,
    onTogglePin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = remember(category) { ToolRegistry.getByCategory(category) }
    var selectedToolForAction by remember { mutableStateOf<ToolDefinition?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(category.titleRes),
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QuackyBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools, key = { it.id }) { tool ->
                ToolTile(
                    tool = tool,
                    onClick = { onNavigateToTool(tool) },
                    onLongClick = { selectedToolForAction = tool }
                )
            }
        }
    }

    selectedToolForAction?.let { tool ->
        ToolActionSheet(
            tool = tool,
            isPinned = isToolPinned(tool.id),
            onDismiss = { selectedToolForAction = null },
            onOpen = { onNavigateToTool(tool) },
            onTogglePin = { onTogglePin(tool.id) },
            onHowToUse = { onOpenHowToUse(tool) }
        )
    }
}
