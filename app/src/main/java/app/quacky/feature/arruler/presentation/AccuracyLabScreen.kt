package app.quacky.feature.arruler.presentation

import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.feature.arruler.engine.ReferenceItem
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class AccuracyTrial(
    val trialNumber: Int,
    val referenceLengthMeters: Double,
    val measuredLengthMeters: Double,
    val errorPercent: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccuracyLabScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedReference by remember { mutableStateOf(ReferenceItem.BANK_CARD) }
    var customRefInput by remember { mutableStateOf("0.100") }
    var selectedSurface by remember { mutableStateOf("Wall") }
    var selectedDistance by remember { mutableStateOf("1.0 m") }
    var selectedLight by remember { mutableStateOf("Bright (~400 lux)") }

    var trialInputText by remember { mutableStateOf("") }
    val trials = remember { mutableStateListOf<AccuracyTrial>() }

    val activeRefLength = if (selectedReference == ReferenceItem.CUSTOM) {
        customRefInput.toDoubleOrNull() ?: 0.100
    } else {
        selectedReference.lengthMeters
    }

    val errors = trials.map { it.errorPercent }
    val meanError = if (errors.isNotEmpty()) errors.average() else 0.0
    val stdDev = if (errors.size >= 2) {
        val variance = errors.map { (it - meanError).pow(2.0) }.average()
        sqrt(variance)
    } else 0.0
    val worstCase = if (errors.isNotEmpty()) errors.maxOrNull() ?: 0.0 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Accuracy Lab",
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (trials.isNotEmpty()) {
                        IconButton(onClick = {
                            val csv = buildString {
                                appendLine("Device,${Build.MANUFACTURER} ${Build.MODEL}")
                                appendLine("Reference,${selectedReference.label}")
                                appendLine("RefLengthMeters,$activeRefLength")
                                appendLine("Surface,$selectedSurface")
                                appendLine("Distance,$selectedDistance")
                                appendLine("Light,$selectedLight")
                                appendLine()
                                appendLine("Trial,Measured_m,Error_pct")
                                trials.forEach {
                                    appendLine("${it.trialNumber},${it.measuredLengthMeters},${String.format("%.2f", it.errorPercent)}")
                                }
                                appendLine()
                                appendLine("Mean_Error_pct,${String.format("%.2f", meanError)}")
                                appendLine("StdDev_pct,${String.format("%.2f", stdDev)}")
                                appendLine("WorstCase_pct,${String.format("%.2f", worstCase)}")
                            }

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Quacky AR Ruler Accuracy Benchmark")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export CSV Report"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export CSV",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reference item selection
            item {
                Text(
                    text = "1. Reference Standard",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReferenceItem.entries.take(3).forEach { item ->
                        val isSelected = selectedReference == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color(0xFF1E1E1E))
                                .clickable { selectedReference = item }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label.split(" ").first(),
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // Environment selection
            item {
                Text(
                    text = "2. Test Conditions",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Wall", "Floor", "Table", "Laptop", "Fridge").forEach { surface ->
                        val isSelected = selectedSurface == surface
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color(0xFF1E1E1E))
                                .clickable { selectedSurface = surface }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = surface,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // Trial input
            item {
                Text(
                    text = "3. Record Measurements (5 Trials)",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = trialInputText,
                        onValueChange = { trialInputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. 0.086", color = Color.Gray, fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    QuackyButton(
                        onClick = {
                            val measured = trialInputText.toDoubleOrNull()
                            if (measured != null && measured > 0) {
                                val err = abs(measured - activeRefLength) / activeRefLength * 100.0
                                trials.add(
                                    AccuracyTrial(
                                        trialNumber = trials.size + 1,
                                        referenceLengthMeters = activeRefLength,
                                        measuredLengthMeters = measured,
                                        errorPercent = err
                                    )
                                )
                                trialInputText = ""
                            }
                        },
                        style = QuackyButtonStyle.Primary
                    ) {
                        Text("Add", color = Color.Black)
                    }
                }
            }

            // Results Card
            if (trials.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF282828), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Benchmark Summary",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Mean Error", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        String.format("%.2f%%", meanError),
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (meanError <= 1.0) Color.Green else Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                                Column {
                                    Text("Std Dev", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        String.format("%.2f%%", stdDev),
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                                Column {
                                    Text("Worst Case", color = Color.Gray, fontSize = 12.sp)
                                    Text(
                                        String.format("%.2f%%", worstCase),
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (worstCase <= 2.0) Color.Green else Color(0xFFFFB74D),
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Trial list
                itemsIndexed(trials) { idx, trial ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181818))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trial #${trial.trialNumber}: ${String.format("%.4f m", trial.measuredLengthMeters)}",
                            fontFamily = SatoshiFontFamily,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = String.format("Δ %.2f%%", trial.errorPercent),
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (trial.errorPercent <= 1.0) Color.Green else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
