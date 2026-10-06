package app.quacky.feature.areavolume.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import androidx.compose.material3.AlertDialog
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyCard
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry
import app.quacky.feature.areavolume.domain.LengthUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaVolumeScreen(
    viewModel: AreaVolumeViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    ToolScaffold(
        tool = ToolRegistry.AREA_VOLUME,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(QuackyBackground)
        ) {
            // Top Tabs: Area | Volume | Estimate
            TabRow(
                selectedTabIndex = state.activeTab,
                containerColor = QuackySurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[state.activeTab]),
                        color = Color.White,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = state.activeTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text(stringResource(R.string.areavolume_tab_area), fontWeight = if (state.activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = state.activeTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text(stringResource(R.string.areavolume_tab_volume), fontWeight = if (state.activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = state.activeTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text(stringResource(R.string.areavolume_tab_estimate), fontWeight = if (state.activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Shape Selector Chips
            val shapeChips = when (state.activeTab) {
                0 -> listOf(
                    "rectangle" to "Rectangle",
                    "square" to "Square",
                    "triangle" to "Triangle",
                    "circle" to "Circle",
                    "semicircle" to "Semicircle",
                    "trapezoid" to "Trapezoid",
                    "parallelogram" to "Parallelogram",
                    "ellipse" to "Ellipse",
                    "ring" to "Ring",
                    "irregular_polygon" to "Irregular Polygon"
                )
                1 -> listOf(
                    "cube" to "Cube",
                    "cuboid" to "Cuboid",
                    "cylinder" to "Cylinder",
                    "cone" to "Cone",
                    "sphere" to "Sphere",
                    "hemisphere" to "Hemisphere",
                    "pyramid" to "Pyramid",
                    "capsule" to "Capsule"
                )
                else -> listOf(
                    "paint" to "Paint",
                    "tile" to "Tiles",
                    "concrete" to "Concrete",
                    "tank" to "Water Tank"
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shapeChips) { (key, label) ->
                    val isSelected = state.activeShape == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectShape(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = QuackySurface,
                            labelColor = QuackyTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.White else QuackyOutline
                        )
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Diagram or Irregular Polygon Canvas
                item {
                    if (state.activeShape == "irregular_polygon") {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tap canvas to add corners (${state.irregularPoints.size} points)", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = QuackyTextSecondary)
                                Row {
                                    IconButton(onClick = { viewModel.undoIrregularPoint() }) {
                                        Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", tint = QuackyTextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.clearIrregularPoints() }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = QuackyTextTertiary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            IrregularPolygonCanvas(
                                points = state.irregularPoints,
                                onAddPoint = { viewModel.addIrregularPoint(it) }
                            )
                        }
                    } else {
                        ShapeDiagram(shapeKey = state.activeShape)
                    }
                }

                // Input fields
                val fields = getFieldsForShape(state.activeShape)
                items(fields) { fieldKey ->
                    InputFieldWithUnit(
                        label = fieldKey.replaceFirstChar { it.uppercase() },
                        value = state.inputs[fieldKey] ?: "0",
                        unit = state.units[fieldKey] ?: LengthUnit.M,
                        canImportAr = state.canImportAr,
                        onValueChange = { viewModel.updateInput(fieldKey, it) },
                        onUnitChange = { viewModel.updateUnit(fieldKey, it) },
                        onImportAr = { viewModel.openArChoiceDialog(fieldKey) }
                    )
                }

                // Results Card
                item {
                    val result = state.result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = QuackySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = result.primaryLabel,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = QuackyTextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = result.primaryValue,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QuackyTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.primaryUnit,
                                    fontSize = 16.sp,
                                    color = QuackyTextSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }

                            result.secondaryLabel?.let { secLabel ->
                                Text(
                                    text = "$secLabel: ${result.secondaryValue} ${result.secondaryUnit}",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = QuackyTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Show all units expander
                            if (result.allUnits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleShowAllUnits() }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.showAllUnits) stringResource(R.string.areavolume_hide_all_units) else stringResource(R.string.areavolume_show_all_units),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = if (state.showAllUnits) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (state.showAllUnits) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(QuackyBackground)
                                            .padding(10.dp)
                                    ) {
                                        result.allUnits.forEach { (unitStr, valStr) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(unitStr, fontSize = 11.sp, color = QuackyTextSecondary)
                                                Text(valStr, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = QuackyTextPrimary)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Save calculation button
                            Button(
                                onClick = { viewModel.saveCalculation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QuackySurfaceElevated,
                                    contentColor = QuackyTextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.areavolume_save_calc), fontSize = 12.sp)
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

    state.activeArChoiceField?.let { fieldKey ->
        AlertDialog(
            onDismissRequest = { viewModel.closeArChoiceDialog() },
            containerColor = QuackySurfaceElevated,
            title = {
                Text(
                    text = "Import AR Measurement",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select a saved measurement for ${fieldKey.replaceFirstChar { it.uppercase() }}:",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        color = QuackyTextSecondary
                    )
                    state.availableArChoices.forEach { choice ->
                        QuackyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.importArMeasurement(fieldKey, choice.lengthMeters) }
                        ) {
                            Text(
                                text = choice.label,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = QuackyTextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                QuackyButton(
                    onClick = { viewModel.closeArChoiceDialog() },
                    style = QuackyButtonStyle.Secondary
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun InputFieldWithUnit(
    label: String,
    value: String,
    unit: LengthUnit,
    canImportAr: Boolean,
    onValueChange: (String) -> Unit,
    onUnitChange: (LengthUnit) -> Unit,
    onImportAr: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = QuackyOutline,
                focusedTextColor = QuackyTextPrimary,
                unfocusedTextColor = QuackyTextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        // Unit selector dropdown
        Box {
            Button(
                onClick = { expanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = QuackySurface, contentColor = QuackyTextPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(56.dp)
                    .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
            ) {
                Text(unit.symbol, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(QuackySurfaceElevated)
            ) {
                LengthUnit.entries.forEach { u ->
                    DropdownMenuItem(
                        text = { Text("${u.symbol} (${u.name.lowercase()})", color = QuackyTextPrimary) },
                        onClick = {
                            onUnitChange(u)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Hardware honesty AR import button (shown ONLY if AR is available and measurements exist)
        if (canImportAr) {
            IconButton(
                onClick = onImportAr,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(QuackySurface)
                    .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Rounded.Straighten, contentDescription = "Use AR measurement", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun getFieldsForShape(shape: String): List<String> {
    return when (shape) {
        "rectangle" -> listOf("length", "width")
        "square" -> listOf("side")
        "triangle" -> listOf("base", "height")
        "circle", "semicircle" -> listOf("radius")
        "trapezoid" -> listOf("baseA", "baseB", "height")
        "parallelogram" -> listOf("base", "height")
        "ellipse" -> listOf("radiusA", "radiusB")
        "ring" -> listOf("outerRadius", "innerRadius")
        "cube" -> listOf("side")
        "cuboid" -> listOf("length", "width", "height")
        "cylinder", "cone", "capsule" -> listOf("radius", "height")
        "sphere", "hemisphere" -> listOf("radius")
        "pyramid" -> listOf("length", "width", "height")
        "paint" -> listOf("length", "height")
        "tile" -> listOf("length", "width", "tileWidth", "tileLength")
        "concrete" -> listOf("length", "width", "thickness")
        "tank" -> listOf("radius", "height")
        else -> emptyList()
    }
}
