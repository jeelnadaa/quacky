package app.quacky.feature.colorpicker.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.colorpicker.domain.ColorFormats
import app.quacky.feature.colorpicker.domain.ColorMath
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorReadoutSheet(
    pin: ColorPin,
    formats: ColorFormats,
    contrastColor: Int,
    sheetState: SheetState,
    onSaveToHistory: () -> Unit,
    onSelectComparisonColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val (tints, shades) = remember(pin.color) { ColorMath.generateTintsAndShades(pin.color) }
    val harmonies = remember(pin.color) { ColorMath.generateHarmonies(pin.color) }
    val contrastResult = remember(pin.color, contrastColor) {
        ColorMath.calculateContrast(pin.color, contrastColor)
    }

    fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuackySurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Color Inspection",
                    color = QuackyTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = QuackyTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Swatch & Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(pin.color))
                        .border(1.5.dp, QuackyOutline, RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pin.colorName,
                        color = QuackyTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pin.hex,
                        color = QuackyTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                QuackyButton(
                    onClick = onSaveToHistory,
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Rounded.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Format Values Table
            Text("VALUES", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(QuackyBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, QuackyOutline, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ColorValueRow("HEX", formats.hex) { copyText("HEX", formats.hex) }
                ColorValueRow("RGB", "${formats.rgb.first}, ${formats.rgb.second}, ${formats.rgb.third}") {
                    copyText("RGB", "rgb(${formats.rgb.first}, ${formats.rgb.second}, ${formats.rgb.third})")
                }
                ColorValueRow("HSL", String.format(Locale.US, "%.0f°, %.0f%%, %.0f%%", formats.hsl.first, formats.hsl.second, formats.hsl.third)) {
                    copyText("HSL", "hsl(${formats.hsl.first.toInt()}, ${formats.hsl.second.toInt()}%, ${formats.hsl.third.toInt()}%)")
                }
                ColorValueRow("HSV", String.format(Locale.US, "%.0f°, %.0f%%, %.0f%%", formats.hsv.first, formats.hsv.second, formats.hsv.third)) {
                    copyText("HSV", "${formats.hsv.first.toInt()}°, ${formats.hsv.second.toInt()}%, ${formats.hsv.third.toInt()}%")
                }
                ColorValueRow("CMYK", "${formats.cmyk[0]}%, ${formats.cmyk[1]}%, ${formats.cmyk[2]}%, ${formats.cmyk[3]}%") {
                    copyText("CMYK", "cmyk(${formats.cmyk.joinToString(", ")}%)")
                }
                ColorValueRow("Compose", formats.composeString) { copyText("Compose", formats.composeString) }
                ColorValueRow("CSS", formats.cssRgb) { copyText("CSS", formats.cssRgb) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tints and Shades
            Text("TINTS & SHADES", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shades.reversed().forEach { c ->
                    ColorBox(c, modifier = Modifier.weight(1f))
                }
                ColorBox(pin.color, modifier = Modifier.weight(1f), isSelected = true)
                tints.forEach { c ->
                    ColorBox(c, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Harmonies
            Text("HARMONIES", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            harmonies.forEach { (category, colors) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, color = QuackyTextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { c ->
                            ColorBox(c, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // WCAG Contrast Checker
            Text("WCAG CONTRAST CHECKER", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = QuackyBackground,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "Ratio: %.2f : 1", contrastResult.ratio),
                            color = QuackyTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Selector for comparison color
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuackyChip(
                                text = "White",
                                selected = contrastColor == android.graphics.Color.WHITE,
                                onClick = { onSelectComparisonColor(android.graphics.Color.WHITE) }
                            )
                            QuackyChip(
                                text = "Black",
                                selected = contrastColor == android.graphics.Color.BLACK,
                                onClick = { onSelectComparisonColor(android.graphics.Color.BLACK) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ContrastBadge("Normal AA", contrastResult.normalAa)
                        ContrastBadge("Normal AAA", contrastResult.normalAaa)
                        ContrastBadge("Large AA", contrastResult.largeAa)
                        ContrastBadge("Large AAA", contrastResult.largeAaa)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ColorValueRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = QuackyTextTertiary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
        Text(
            value,
            color = QuackyTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "Copy",
            tint = QuackyTextTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ColorBox(colorInt: Int, modifier: Modifier = Modifier, isSelected: Boolean = false) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(colorInt))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) QuackyAccent else QuackyOutline,
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun ContrastBadge(label: String, pass: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (pass) QuackyAccent else QuackySurfaceElevated)
            .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: ${if (pass) "PASS" else "FAIL"}",
            color = if (pass) Color.Black else QuackyTextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
