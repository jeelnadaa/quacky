package app.quacky.feature.qrgenerator.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import app.quacky.feature.qrgenerator.domain.EcLevel
import app.quacky.feature.qrgenerator.domain.GeneratorFormat
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrGeneratorScreen(
    viewModel: QrGeneratorViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        val msg = uiState.errorMessage ?: uiState.infoMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuackyBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = QuackySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = QuackyTextPrimary
                        )
                    }

                    Text(
                        text = stringResource(R.string.tool_qr_generator_name),
                        color = QuackyTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onOpenHowToUse) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = stringResource(R.string.action_how_to_use),
                            tint = QuackyTextSecondary
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Input Type Selector
                Text(
                    text = "DATA TYPE",
                    color = QuackyTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(InputType.entries) { type ->
                        QuackyChip(
                            text = type.label,
                            selected = uiState.inputType == type,
                            onClick = { viewModel.setInputType(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Output Format Selector
                Text(
                    text = "BARCODE FORMAT",
                    color = QuackyTextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GeneratorFormat.entries) { fmt ->
                        QuackyChip(
                            text = fmt.displayName,
                            selected = uiState.format == fmt,
                            onClick = { viewModel.setFormat(fmt) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form Fields based on InputType
                GeneratorFormFields(
                    uiState = uiState,
                    onUpdateField = { field, value -> viewModel.updateField(field, value) },
                    onToggleWifiHidden = { viewModel.toggleWifiHidden() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Live Preview Card
                Surface(
                    color = QuackySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            color = QuackyTextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val preview = uiState.previewBitmap
                        if (preview != null) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = preview.asImageBitmap(),
                                    contentDescription = "Generated barcode preview",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(QuackySurfaceElevated, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.errorMessage ?: "Enter valid data to generate",
                                    color = QuackyTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Error Correction chips for QR
                        if (uiState.format == GeneratorFormat.QR_CODE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Error correction:", color = QuackyTextTertiary, fontSize = 11.sp)
                                EcLevel.entries.forEach { level ->
                                    QuackyChip(
                                        text = level.name,
                                        selected = uiState.ecLevel == level,
                                        onClick = { viewModel.setEcLevel(level) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuackyButton(
                        onClick = { viewModel.saveToGallery { } },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PNG")
                    }

                    QuackyButton(
                        onClick = {
                            viewModel.exportPdf { pdfBytes ->
                                val cacheFile = File(context.cacheDir, "quacky_code.pdf")
                                cacheFile.writeBytes(pdfBytes)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                            }
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            viewModel.exportSvg { svgStr ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("SVG", svgStr))
                                Toast.makeText(context, "SVG markup copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy SVG")
                    }

                    QuackyButton(
                        onClick = {
                            val payload = viewModel.getPayloadString()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Payload", payload))
                            Toast.makeText(context, "Payload text copied", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_copy))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun GeneratorFormFields(
    uiState: QrGeneratorUiState,
    onUpdateField: (String, String) -> Unit,
    onToggleWifiHidden: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = QuackyAccent,
        unfocusedBorderColor = QuackyOutline,
        focusedTextColor = QuackyTextPrimary,
        unfocusedTextColor = QuackyTextPrimary,
        focusedLabelColor = QuackyAccent,
        unfocusedLabelColor = QuackyTextSecondary
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (uiState.inputType) {
            InputType.TEXT -> {
                OutlinedTextField(
                    value = uiState.textValue,
                    onValueChange = { onUpdateField("text", it) },
                    label = { Text("Text content") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.URL -> {
                OutlinedTextField(
                    value = uiState.urlValue,
                    onValueChange = { onUpdateField("url", it) },
                    label = { Text("URL (https://…)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.WIFI -> {
                OutlinedTextField(
                    value = uiState.wifiSsid,
                    onValueChange = { onUpdateField("wifiSsid", it) },
                    label = { Text("Network name (SSID)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.wifiPassword,
                    onValueChange = { onUpdateField("wifiPassword", it) },
                    label = { Text("Password") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hidden network", color = QuackyTextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = uiState.wifiHidden,
                        onCheckedChange = { onToggleWifiHidden() },
                        colors = SwitchDefaults.colors(checkedThumbColor = QuackyAccent)
                    )
                }
            }
            InputType.CONTACT -> {
                OutlinedTextField(
                    value = uiState.contactName,
                    onValueChange = { onUpdateField("contactName", it) },
                    label = { Text("Full Name") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.contactPhone,
                    onValueChange = { onUpdateField("contactPhone", it) },
                    label = { Text("Phone Number") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.contactEmail,
                    onValueChange = { onUpdateField("contactEmail", it) },
                    label = { Text("Email Address") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.contactOrg,
                    onValueChange = { onUpdateField("contactOrg", it) },
                    label = { Text("Organization") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.EMAIL -> {
                OutlinedTextField(
                    value = uiState.emailTo,
                    onValueChange = { onUpdateField("emailTo", it) },
                    label = { Text("Recipient Email") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.emailSubject,
                    onValueChange = { onUpdateField("emailSubject", it) },
                    label = { Text("Subject") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.emailBody,
                    onValueChange = { onUpdateField("emailBody", it) },
                    label = { Text("Message Body") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.SMS -> {
                OutlinedTextField(
                    value = uiState.smsPhone,
                    onValueChange = { onUpdateField("smsPhone", it) },
                    label = { Text("Phone Number") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.smsMessage,
                    onValueChange = { onUpdateField("smsMessage", it) },
                    label = { Text("Message") },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.PHONE -> {
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = { onUpdateField("phoneNumber", it) },
                    label = { Text("Phone Number") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.UPI -> {
                OutlinedTextField(
                    value = uiState.upiVpa,
                    onValueChange = { onUpdateField("upiVpa", it) },
                    label = { Text("UPI ID (e.g. user@bank)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.upiName,
                    onValueChange = { onUpdateField("upiName", it) },
                    label = { Text("Payee Name") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.upiAmount,
                    onValueChange = { onUpdateField("upiAmount", it) },
                    label = { Text("Amount (INR, optional)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.upiNote,
                    onValueChange = { onUpdateField("upiNote", it) },
                    label = { Text("Note (optional)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.LOCATION -> {
                OutlinedTextField(
                    value = uiState.locLat,
                    onValueChange = { onUpdateField("locLat", it) },
                    label = { Text("Latitude (e.g. 37.7749)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.locLng,
                    onValueChange = { onUpdateField("locLng", it) },
                    label = { Text("Longitude (e.g. -122.4194)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            InputType.EVENT -> {
                OutlinedTextField(
                    value = uiState.eventTitle,
                    onValueChange = { onUpdateField("eventTitle", it) },
                    label = { Text("Event Title") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.eventLocation,
                    onValueChange = { onUpdateField("eventLocation", it) },
                    label = { Text("Location (optional)") },
                    colors = textFieldColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
