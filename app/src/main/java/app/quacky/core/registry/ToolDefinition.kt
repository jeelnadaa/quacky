package app.quacky.core.registry

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Metadata definition for every tool in Quacky.
 * Home, search, categories, pinned tools, and shortcuts all read from this definition.
 */
data class ToolDefinition(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val category: ToolCategory,
    val icon: ImageVector,
    val keywords: List<String>,
    val route: String,
    val requirements: Set<ToolRequirement> = emptySet()
)
