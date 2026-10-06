package app.quacky.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import app.quacky.core.components.ToolTile
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolDefinition
import app.quacky.core.registry.ToolRegistry
import app.quacky.core.tips.GuideRegistry
import app.quacky.core.tips.HowToSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllGuidesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedToolForGuide by remember { mutableStateOf<ToolDefinition?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = QuackyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "How-To Guides",
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
            ToolCategory.entries.forEach { category ->
                val tools = ToolRegistry.getByCategory(category)
                item(key = category.id) {
                    SectionLabel(text = stringResource(category.titleRes))
                }
                items(tools, key = { it.id }) { tool ->
                    ToolTile(
                        tool = tool,
                        onClick = { selectedToolForGuide = tool },
                        onLongClick = { selectedToolForGuide = tool },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    selectedToolForGuide?.let { tool ->
        val guide = remember(tool) { GuideRegistry.getGuideForTool(tool.id) }
        HowToSheet(
            guide = guide,
            onDismiss = { selectedToolForGuide = null }
        )
    }
}
