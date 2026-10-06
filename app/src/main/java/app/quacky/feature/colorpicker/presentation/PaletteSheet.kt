package app.quacky.feature.colorpicker.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackySurfaceElevated
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.data.local.db.entity.SavedPaletteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteSheet(
    pins: List<ColorPin>,
    savedPalettes: List<SavedPaletteEntity>,
    sheetState: SheetState,
    onSaveCurrentPalette: (String) -> Unit,
    onDeletePalette: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var paletteNameInput by remember { mutableStateOf("") }

    fun copyText(label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, content))
        Toast.makeText(context, "Copied $label snippet", Toast.LENGTH_SHORT).show()
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
                    text = "Palettes",
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

            // Current Pins Strip
            Text("CURRENT PINS (${pins.size})", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pins.forEach { pin ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(pin.color))
                            .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save Palette Form
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = paletteNameInput,
                    onValueChange = { paletteNameInput = it },
                    placeholder = { Text("Palette name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QuackyAccent,
                        unfocusedBorderColor = QuackyOutline,
                        focusedTextColor = QuackyTextPrimary,
                        unfocusedTextColor = QuackyTextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                QuackyButton(
                    onClick = {
                        onSaveCurrentPalette(paletteNameInput)
                        paletteNameInput = ""
                    },
                    style = QuackyButtonStyle.Primary,
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Options
            Text("EXPORT CURRENT PALETTE", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuackyButton(
                    onClick = {
                        val json = buildPaletteJson(pins)
                        copyText("JSON", json)
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("JSON", fontSize = 12.sp)
                }

                QuackyButton(
                    onClick = {
                        val css = buildPaletteCss(pins)
                        copyText("CSS", css)
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("CSS", fontSize = 12.sp)
                }

                QuackyButton(
                    onClick = {
                        val xml = buildPaletteXml(pins)
                        copyText("colors.xml", xml)
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Android XML", fontSize = 12.sp)
                }

                QuackyButton(
                    onClick = {
                        val tailwind = buildPaletteTailwind(pins)
                        copyText("Tailwind", tailwind)
                    },
                    style = QuackyButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tailwind", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Palettes List
            Text("SAVED PALETTES", color = QuackyTextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (savedPalettes.isEmpty()) {
                Text(
                    text = "No saved palettes yet",
                    color = QuackyTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedPalettes.forEach { palette ->
                        SavedPaletteRow(
                            palette = palette,
                            onDelete = { onDeletePalette(palette.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SavedPaletteRow(palette: SavedPaletteEntity, onDelete: () -> Unit) {
    val dateStr = remember(palette.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(palette.createdAt))
    }

    Surface(
        color = QuackySurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = palette.name,
                    color = QuackyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateStr,
                    color = QuackyTextTertiary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = QuackyTextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun buildPaletteJson(pins: List<ColorPin>): String {
    val sb = StringBuilder("[\n")
    pins.forEachIndexed { i, p ->
        sb.append("""  {"name": "${p.colorName}", "hex": "${p.hex}"}""")
        if (i < pins.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("]")
    return sb.toString()
}

private fun buildPaletteCss(pins: List<ColorPin>): String {
    val sb = StringBuilder(":root {\n")
    pins.forEachIndexed { i, p ->
        val safeName = p.colorName.lowercase().replace(" ", "-")
        sb.append("  --color-$safeName: ${p.hex};\n")
    }
    sb.append("}")
    return sb.toString()
}

private fun buildPaletteXml(pins: List<ColorPin>): String {
    val sb = StringBuilder("<resources>\n")
    pins.forEach { p ->
        val safeName = p.colorName.lowercase().replace(" ", "_")
        sb.append("""    <color name="$safeName">${p.hex}</color>""").append("\n")
    }
    sb.append("</resources>")
    return sb.toString()
}

private fun buildPaletteTailwind(pins: List<ColorPin>): String {
    val sb = StringBuilder("colors: {\n")
    pins.forEach { p ->
        val safeName = p.colorName.lowercase().replace(" ", "-")
        sb.append("""  '$safeName': '${p.hex}',""").append("\n")
    }
    sb.append("}")
    return sb.toString()
}
