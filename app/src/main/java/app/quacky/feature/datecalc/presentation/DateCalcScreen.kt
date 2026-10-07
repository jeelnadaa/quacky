package app.quacky.feature.datecalc.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.component.QuackyButton
import app.quacky.core.designsystem.component.QuackyButtonStyle
import app.quacky.core.designsystem.component.QuackyChip
import app.quacky.core.designsystem.component.SectionLabel
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolRegistry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalcScreen(
    viewModel: DateCalcViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val countdowns by viewModel.savedCountdowns.collectAsState()
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    ToolScaffold(
        tool = ToolRegistry.DATE_CALC,
        onBack = onBack,
        onHelpClick = onOpenHowToUse,
        onResetClick = viewModel::reset
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pinned Countdowns (if any exist)
            if (countdowns.isNotEmpty()) {
                item {
                    SectionLabel(text = "Saved Countdowns")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(countdowns, key = { it.id }) { item ->
                            val targetDate = LocalDate.ofEpochDay(item.targetEpochDay)
                            val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
                            Surface(
                                shape = RoundedCornerShape(CardCornerRadius),
                                color = QuackySurface,
                                border = BorderStroke(1.dp, QuackyOutline),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = item.title,
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = QuackyTextPrimary
                                    )
                                    Text(
                                        text = if (daysLeft >= 0) "$daysLeft days left" else "${-daysLeft} days ago",
                                        fontFamily = SatoshiFontFamily,
                                        fontSize = 12.sp,
                                        color = QuackyTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs Selector
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        QuackyChip(
                            text = "Age",
                            selected = state.selectedTab == DateTab.AGE,
                            onClick = { viewModel.selectTab(DateTab.AGE) }
                        )
                    }
                    item {
                        QuackyChip(
                            text = "Difference",
                            selected = state.selectedTab == DateTab.DIFFERENCE,
                            onClick = { viewModel.selectTab(DateTab.DIFFERENCE) }
                        )
                    }
                    item {
                        QuackyChip(
                            text = "Add / Subtract",
                            selected = state.selectedTab == DateTab.ADD_SUBTRACT,
                            onClick = { viewModel.selectTab(DateTab.ADD_SUBTRACT) }
                        )
                    }
                    item {
                        QuackyChip(
                            text = "Day Info",
                            selected = state.selectedTab == DateTab.DAY_INFO,
                            onClick = { viewModel.selectTab(DateTab.DAY_INFO) }
                        )
                    }
                }
            }

            // Content according to selected tab
            when (state.selectedTab) {
                DateTab.AGE -> {
                    item {
                        SectionLabel(text = "Date of Birth")
                        DateSelectorCard(
                            label = "Birth Date",
                            date = state.birthDate,
                            onDateChange = viewModel::setBirthDate
                        )
                    }
                    item {
                        SectionLabel(text = "Results")
                        Surface(
                            shape = RoundedCornerShape(CardCornerRadius),
                            color = QuackySurface,
                            border = BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${state.ageResult.years} years, ${state.ageResult.months} months, ${state.ageResult.days} days",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = QuackyTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Next birthday in ${state.ageResult.nextBirthdayDays} days (${state.ageResult.nextBirthdayWeekday})",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 14.sp,
                                    color = QuackyTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Total: ${state.ageResult.totalMonths} months · ${state.ageResult.totalWeeks} weeks · ${state.ageResult.totalDays} days",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 13.sp,
                                    color = QuackyTextTertiary
                                )
                            }
                        }
                    }
                }
                DateTab.DIFFERENCE -> {
                    item {
                        SectionLabel(text = "Date Range")
                        DateSelectorCard(
                            label = "Start Date",
                            date = state.diffStartDate,
                            onDateChange = viewModel::setDiffStartDate
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DateSelectorCard(
                            label = "End Date",
                            date = state.diffEndDate,
                            onDateChange = viewModel::setDiffEndDate
                        )
                    }
                    item {
                        SectionLabel(text = "Results")
                        Surface(
                            shape = RoundedCornerShape(CardCornerRadius),
                            color = QuackySurface,
                            border = BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${state.diffResult.years} years, ${state.diffResult.months} months, ${state.diffResult.days} days",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = QuackyTextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Total: ${state.diffResult.totalDays} calendar days (${state.diffResult.totalWeeks} weeks)",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 14.sp,
                                    color = QuackyTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Business days (Mon–Fri): ${state.diffResult.businessDays} days",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 13.sp,
                                    color = QuackyTextTertiary
                                )
                            }
                        }
                    }
                }
                DateTab.ADD_SUBTRACT -> {
                    item {
                        SectionLabel(text = "Start Date")
                        DateSelectorCard(
                            label = "Date",
                            date = state.addSubStartDate,
                            onDateChange = viewModel::setAddSubStartDate
                        )
                    }
                    item {
                        SectionLabel(text = "Result Date")
                        Surface(
                            shape = RoundedCornerShape(CardCornerRadius),
                            color = QuackySurface,
                            border = BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = state.addSubResultDate.format(formatter),
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = QuackyTextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${state.addSubResultDate.dayOfWeek}",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 15.sp,
                                    color = QuackyTextSecondary
                                )
                            }
                        }
                    }
                }
                DateTab.DAY_INFO -> {
                    item {
                        SectionLabel(text = "Selected Date")
                        DateSelectorCard(
                            label = "Date",
                            date = state.infoDate,
                            onDateChange = viewModel::setInfoDate
                        )
                    }
                    item {
                        SectionLabel(text = "Day Details")
                        Surface(
                            shape = RoundedCornerShape(CardCornerRadius),
                            color = QuackySurface,
                            border = BorderStroke(1.dp, QuackyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoRow("Weekday", "${state.dayInfoResult.dayOfWeek}")
                                InfoRow("Day of Year", "Day ${state.dayInfoResult.dayOfYear}")
                                InfoRow("Week Number (ISO)", "Week ${state.dayInfoResult.isoWeekNumber}")
                                InfoRow("Quarter", "Q${state.dayInfoResult.quarter}")
                                InfoRow("Leap Year", if (state.dayInfoResult.isLeapYear) "Yes" else "No")
                                InfoRow("Days Remaining in Year", "${state.dayInfoResult.daysRemainingInYear} days")
                            }
                        }
                    }
                }
            }

            // Quick Actions: Copy and Save to history
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuackyButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val summary = when (state.selectedTab) {
                                DateTab.AGE -> "Age: ${state.ageResult.years}y ${state.ageResult.months}m ${state.ageResult.days}d"
                                DateTab.DIFFERENCE -> "Difference: ${state.diffResult.totalDays} days (${state.diffResult.businessDays} business days)"
                                DateTab.ADD_SUBTRACT -> "Result: ${state.addSubResultDate.format(formatter)}"
                                DateTab.DAY_INFO -> "Day: ${state.dayInfoResult.dayOfWeek}, Q${state.dayInfoResult.quarter}"
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Quacky Date", summary))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Secondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy", fontSize = 13.sp)
                    }

                    QuackyButton(
                        onClick = {
                            viewModel.saveCalculationToHistory()
                            Toast.makeText(context, "Saved to history", Toast.LENGTH_SHORT).show()
                        },
                        style = QuackyButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save", fontSize = 13.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun DateSelectorCard(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    val openDatePicker = {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateChange(LocalDate.of(year, month + 1, dayOfMonth))
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        ).show()
    }

    Surface(
        shape = RoundedCornerShape(CardCornerRadius),
        color = QuackySurface,
        border = BorderStroke(1.dp, QuackyOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openDatePicker() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    color = QuackyTextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = date.format(formatter),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = QuackyTextPrimary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuackyButton(
                    onClick = openDatePicker,
                    style = QuackyButtonStyle.Secondary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = "Choose Date", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Change", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontFamily = SatoshiFontFamily, fontSize = 14.sp, color = QuackyTextSecondary)
        Text(text = value, fontFamily = SatoshiFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = QuackyTextPrimary)
    }
}
