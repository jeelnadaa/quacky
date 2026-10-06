package app.quacky.feature.screenruler.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import app.quacky.feature.screenruler.domain.RulerMath
import java.util.Locale

@Composable
fun RulerCalibrationDialog(
    initialFactor: Float,
    isCalibrated: Boolean,
    onSave: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val metrics = context.resources.displayMetrics
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val baseDpi = if (isLandscape) metrics.xdpi else metrics.ydpi

    var factor by remember { mutableFloatStateOf(initialFactor) }
    var selectedTab by remember { mutableStateOf(CalibrationMethod.CREDIT_CARD) }
    var knownLengthInput by remember { mutableStateOf("50.0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = QuackySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, QuackyOutline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ruler_calibration_title),
                    color = QuackyTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.ruler_calibration_desc),
                    color = QuackyTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Method Tabs
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = QuackySurfaceElevated,
                    contentColor = QuackyTextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = QuackyAccent
                        )
                    },
                    modifier = Modifier.border(1.dp, QuackyOutline, RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == CalibrationMethod.CREDIT_CARD,
                        onClick = { selectedTab = CalibrationMethod.CREDIT_CARD },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.ruler_credit_card_method), fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == CalibrationMethod.KNOWN_LENGTH,
                        onClick = { selectedTab = CalibrationMethod.KNOWN_LENGTH },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Rounded.Straighten,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.ruler_known_length_method), fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == CalibrationMethod.CREDIT_CARD) {
                    Text(
                        text = stringResource(R.string.ruler_credit_card_hint),
                        color = QuackyTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank card preview outline (85.60 mm x 53.98 mm)
                    val pxPerMm = RulerMath.pixelsPerMm(baseDpi, factor)
                    val cardWidthPx = RulerMath.CREDIT_CARD_WIDTH_MM * pxPerMm
                    val cardHeightPx = RulerMath.CREDIT_CARD_HEIGHT_MM * pxPerMm

                    // Convert to Dp for Compose canvas bounding box
                    val density = context.resources.displayMetrics.density
                    val cardWidthDp = (cardWidthPx / density).dp
                    val cardHeightDp = (cardHeightPx / density).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(QuackyBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(width = cardWidthDp.coerceAtMost(280.dp), height = cardHeightDp.coerceAtMost(160.dp))
                        ) {
                            drawRoundRect(
                                color = QuackyAccent,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 3f)
                            )
                        }
                        Text(
                            text = "85.6 × 54.0 mm",
                            color = QuackyTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider for factor adjustment
                    Text(
                        text = String.format(Locale.US, stringResource(R.string.ruler_scale_factor), factor),
                        color = QuackyTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Slider(
                        value = factor,
                        onValueChange = { factor = it },
                        valueRange = 0.700f..1.300f,
                        colors = SliderDefaults.colors(
                            thumbColor = QuackyAccent,
                            activeTrackColor = QuackyAccent,
                            inactiveTrackColor = QuackyOutline
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuackyButton(
                            onClick = { factor = (factor - 0.005f).coerceAtLeast(0.700f) },
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-0.005")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        QuackyButton(
                            onClick = { factor = (factor + 0.005f).coerceAtMost(1.300f) },
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+0.005")
                        }
                    }
                } else {
                    // Known Length Method
                    Text(
                        text = stringResource(R.string.ruler_known_length_hint),
                        color = QuackyTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val nominalMm = 50.0f
                    val pxPerMm = RulerMath.pixelsPerMm(baseDpi, factor)
                    val linePx = nominalMm * pxPerMm
                    val density = context.resources.displayMetrics.density
                    val lineDp = (linePx / density).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(QuackyBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, QuackyOutline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(width = lineDp.coerceAtMost(280.dp), height = 40.dp)) {
                            val midY = size.height / 2
                            drawLine(QuackyAccent, Offset(0f, midY - 12f), Offset(0f, midY + 12f), 3f)
                            drawLine(QuackyAccent, Offset(size.width, midY - 12f), Offset(size.width, midY + 12f), 3f)
                            drawLine(QuackyAccent, Offset(0f, midY), Offset(size.width, midY), 3f)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = knownLengthInput,
                        onValueChange = { input ->
                            knownLengthInput = input
                            val parsed = input.toFloatOrNull()
                            if (parsed != null && parsed > 5f) {
                                // actual / nominal = correction ratio
                                val calculatedFactor = (nominalMm / parsed) * initialFactor
                                factor = calculatedFactor.coerceIn(0.5f, 2.0f)
                            }
                        },
                        label = { Text("Actual physical length (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuackyAccent,
                            unfocusedBorderColor = QuackyOutline,
                            focusedTextColor = QuackyTextPrimary,
                            unfocusedTextColor = QuackyTextPrimary,
                            focusedLabelColor = QuackyAccent,
                            unfocusedLabelColor = QuackyTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format(Locale.US, stringResource(R.string.ruler_scale_factor), factor),
                        color = QuackyTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCalibrated) {
                        QuackyButton(
                            onClick = {
                                onReset()
                                factor = 1.0f
                            },
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.ruler_reset_calibration))
                        }
                    } else {
                        QuackyButton(
                            onClick = onDismiss,
                            style = QuackyButtonStyle.Secondary,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_close))
                        }
                    }

                    QuackyButton(
                        onClick = { onSave(factor) },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.ruler_save_calibration))
                    }
                }
            }
        }
    }
}
