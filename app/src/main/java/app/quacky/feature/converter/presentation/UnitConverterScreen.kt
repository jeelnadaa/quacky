package app.quacky.feature.converter.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.haptics.rememberQuackyHaptics
import app.quacky.core.registry.ToolRegistry
import app.quacky.feature.converter.model.UnitCategory
import app.quacky.feature.converter.model.UnitItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    viewModel: UnitConverterViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = rememberQuackyHaptics()
    val scope = rememberCoroutineScope()

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Converted Value", text)
        clipboard.setPrimaryClip(clip)
        haptics.click()
    }

    ToolScaffold(
        tool = ToolRegistry.UNIT_CONVERTER,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        onResetClick = {
            haptics.click()
            viewModel.onClear()
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Category Pills (Horizontal Scroll)
            CategoryPillRow(
                selectedCategory = state.category,
                onSelectCategory = { cat ->
                    haptics.click()
                    viewModel.selectCategory(cat)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Conversion Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Input Card
                Box(modifier = Modifier.weight(1f)) {
                    ConversionValueCard(
                        title = "FROM",
                        value = state.inputString,
                        unit = state.fromUnit,
                        isInput = true,
                        onUnitClick = {
                            haptics.click()
                            showFromPicker = true
                        }
                    )
                }

                // Swap Button in between
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(38.dp)
                        .background(QuackySurfaceElevated, CircleShape)
                        .border(1.dp, QuackyOutline, CircleShape)
                        .clickable {
                            haptics.click()
                            viewModel.swapUnits()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.unit_swap),
                        tint = QuackyTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Target Converted Card
                Box(modifier = Modifier.weight(1f)) {
                    ConversionValueCard(
                        title = "TO",
                        value = state.outputString,
                        unit = state.toUnit,
                        isInput = false,
                        onUnitClick = {
                            haptics.click()
                            showToPicker = true
                        },
                        onCopyClick = {
                            copyToClipboard(state.outputString)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keypad & Breakdown Tabs or Keypad Layout
            KeypadGrid(
                onDigit = { d ->
                    haptics.tick()
                    viewModel.onDigitClick(d)
                },
                onDot = {
                    haptics.tick()
                    viewModel.onDotClick()
                },
                onSign = {
                    haptics.tick()
                    viewModel.onToggleSign()
                },
                onBackspace = {
                    haptics.tick()
                    viewModel.onBackspace()
                },
                onClear = {
                    haptics.click()
                    viewModel.onClear()
                },
                onCopy = {
                    copyToClipboard(state.outputString)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // All Units Breakdown Section Title
            Text(
                text = stringResource(R.string.unit_all_conversions),
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = QuackyTextTertiary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Scrollable breakdown list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(state.allBreakdown) { item ->
                    BreakdownRow(
                        item = item,
                        isCurrentTarget = item.unit.id == state.toUnit.id,
                        onCopy = {
                            copyToClipboard(item.displayValue)
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    // Modal Sheet for Unit Selection
    if (showFromPicker) {
        ModalBottomSheet(
            onDismissRequest = { showFromPicker = false },
            sheetState = sheetState,
            containerColor = QuackySurface
        ) {
            UnitPickerSheetContent(
                title = "Select Source Unit",
                units = state.availableUnits,
                selectedUnit = state.fromUnit,
                onSelect = { unit ->
                    viewModel.selectFromUnit(unit)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showFromPicker = false
                    }
                }
            )
        }
    }

    if (showToPicker) {
        ModalBottomSheet(
            onDismissRequest = { showToPicker = false },
            sheetState = sheetState,
            containerColor = QuackySurface
        ) {
            UnitPickerSheetContent(
                title = "Select Target Unit",
                units = state.availableUnits,
                selectedUnit = state.toUnit,
                onSelect = { unit ->
                    viewModel.selectToUnit(unit)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showToPicker = false
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryPillRow(
    selectedCategory: UnitCategory,
    onSelectCategory: (UnitCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UnitCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) QuackyTextPrimary else QuackySurfaceElevated,
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) QuackyTextPrimary else QuackyOutline,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = stringResource(category.titleRes),
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) QuackyBackground else QuackyTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ConversionValueCard(
    title: String,
    value: String,
    unit: UnitItem,
    isInput: Boolean,
    onUnitClick: () -> Unit,
    onCopyClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QuackySurface, RoundedCornerShape(14.dp))
            .border(1.dp, QuackyOutline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = SatoshiFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = QuackyTextTertiary
            )

            // Unit Selector Pill
            Row(
                modifier = Modifier
                    .background(QuackySurfaceElevated, RoundedCornerShape(12.dp))
                    .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
                    .clickable { onUnitClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit.symbol,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuackyTextPrimary
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select unit",
                    tint = QuackyTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontFamily = SatoshiFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isInput) QuackyTextPrimary else Color(0xFF00E676),
                maxLines = 1
            )

            if (onCopyClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.unit_copy_result),
                    tint = QuackyTextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onCopyClick() }
                )
            }
        }
    }
}

@Composable
private fun KeypadGrid(
    onDigit: (String) -> Unit,
    onDot: () -> Unit,
    onSign: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "7", modifier = Modifier.weight(1f)) { onDigit("7") }
            KeypadButton(text = "8", modifier = Modifier.weight(1f)) { onDigit("8") }
            KeypadButton(text = "9", modifier = Modifier.weight(1f)) { onDigit("9") }
            KeypadButton(
                text = "AC",
                modifier = Modifier.weight(1f),
                textColor = Color(0xFFFF5252),
                isAction = true
            ) { onClear() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "4", modifier = Modifier.weight(1f)) { onDigit("4") }
            KeypadButton(text = "5", modifier = Modifier.weight(1f)) { onDigit("5") }
            KeypadButton(text = "6", modifier = Modifier.weight(1f)) { onDigit("6") }
            KeypadIconButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                modifier = Modifier.weight(1f),
                tint = QuackyTextSecondary,
                isAction = true
            ) { onBackspace() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "1", modifier = Modifier.weight(1f)) { onDigit("1") }
            KeypadButton(text = "2", modifier = Modifier.weight(1f)) { onDigit("2") }
            KeypadButton(text = "3", modifier = Modifier.weight(1f)) { onDigit("3") }
            KeypadButton(
                text = "±",
                modifier = Modifier.weight(1f),
                textColor = QuackyTextSecondary,
                isAction = true
            ) { onSign() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeypadButton(text = "0", modifier = Modifier.weight(1f)) { onDigit("0") }
            KeypadButton(text = ".", modifier = Modifier.weight(1f)) { onDot() }
            KeypadIconButton(
                icon = Icons.Rounded.ContentCopy,
                modifier = Modifier.weight(2f),
                tint = Color(0xFF00E676),
                isAction = true
            ) { onCopy() }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = QuackyTextPrimary,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (isAction) QuackySurfaceElevated else QuackySurface,
                RoundedCornerShape(10.dp)
            )
            .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = SatoshiFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = QuackyTextPrimary,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (isAction) QuackySurfaceElevated else QuackySurface,
                RoundedCornerShape(10.dp)
            )
            .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BreakdownRow(
    item: ConvertedUnitResult,
    isCurrentTarget: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrentTarget) Color(0xFF1E281E) else QuackySurface,
                RoundedCornerShape(10.dp)
            )
            .border(
                1.dp,
                if (isCurrentTarget) Color(0xFF00E676) else QuackyOutline,
                RoundedCornerShape(10.dp)
            )
            .clickable { onCopy() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item.unit.name,
                fontFamily = SatoshiFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = QuackyTextPrimary
            )
            Text(
                text = item.unit.symbol,
                fontFamily = SatoshiFontFamily,
                fontSize = 11.sp,
                color = QuackyTextTertiary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.displayValue,
                fontFamily = SatoshiFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentTarget) Color(0xFF00E676) else QuackyTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = QuackyTextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun UnitPickerSheetContent(
    title: String,
    units: List<UnitItem>,
    selectedUnit: UnitItem,
    onSelect: (UnitItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontFamily = SatoshiFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = QuackyTextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(units) { unit ->
                val isSelected = unit.id == selectedUnit.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) QuackySurfaceElevated else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(unit) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = unit.name,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = QuackyTextPrimary
                        )
                        Text(
                            text = unit.symbol,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 12.sp,
                            color = QuackyTextTertiary
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
