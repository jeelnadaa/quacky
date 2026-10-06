package app.quacky.feature.metadata.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.R
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.metadata.domain.CustomRemovalOptions
import app.quacky.feature.metadata.domain.RemovalChoice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanOptionsSheet(
    totalPhotos: Int,
    removalChoice: RemovalChoice,
    customOptions: CustomRemovalOptions,
    onSelectChoice: (RemovalChoice) -> Unit,
    onUpdateCustom: (CustomRemovalOptions) -> Unit,
    onConfirmClean: (cleanAll: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QuackySurface,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Clean metadata",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = QuackyTextPrimary
            )
            Text(
                text = "Select what hidden information to strip from your photos.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = QuackyTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Choice 1: All metadata
            ChoiceCard(
                title = stringResource(R.string.metadata_strip_all),
                subtitle = "Removes GPS, camera info, exposure, and editing history",
                icon = Icons.Rounded.CleaningServices,
                selected = removalChoice == RemovalChoice.ALL,
                onClick = { onSelectChoice(RemovalChoice.ALL) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Choice 2: Location only
            ChoiceCard(
                title = stringResource(R.string.metadata_strip_location),
                subtitle = "Removes coordinates and altitude; keeps camera & capture settings",
                icon = Icons.Rounded.LocationOff,
                selected = removalChoice == RemovalChoice.LOCATION_ONLY,
                onClick = { onSelectChoice(RemovalChoice.LOCATION_ONLY) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Choice 3: Custom
            ChoiceCard(
                title = stringResource(R.string.metadata_strip_custom),
                subtitle = "Select individual categories to strip",
                icon = Icons.Rounded.Tune,
                selected = removalChoice == RemovalChoice.CUSTOM,
                onClick = { onSelectChoice(RemovalChoice.CUSTOM) }
            )

            if (removalChoice == RemovalChoice.CUSTOM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(QuackyBackground)
                        .padding(8.dp)
                ) {
                    CustomCheckboxRow(
                        label = "Location (GPS coordinates & altitude)",
                        checked = customOptions.removeLocation,
                        onCheckedChange = { onUpdateCustom(customOptions.copy(removeLocation = it)) }
                    )
                    CustomCheckboxRow(
                        label = "Device (Make, model, software, lens)",
                        checked = customOptions.removeDevice,
                        onCheckedChange = { onUpdateCustom(customOptions.copy(removeDevice = it)) }
                    )
                    CustomCheckboxRow(
                        label = "Capture (Date, time, ISO, exposure, flash)",
                        checked = customOptions.removeCapture,
                        onCheckedChange = { onUpdateCustom(customOptions.copy(removeCapture = it)) }
                    )
                    CustomCheckboxRow(
                        label = "Other tags (Comments, copyright, software)",
                        checked = customOptions.removeOther,
                        onCheckedChange = { onUpdateCustom(customOptions.copy(removeOther = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Clean copies are saved to Pictures/Quacky/Clean. Originals are never modified.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = QuackyTextTertiary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (totalPhotos > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onConfirmClean(false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QuackyBackground,
                            contentColor = QuackyTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
                    ) {
                        Text("Clean this photo", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onConfirmClean(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Clean all ($totalPhotos)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Button(
                    onClick = { onConfirmClean(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.metadata_clean_action), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) QuackyBackground else QuackySurface)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.5f) else QuackyOutline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else QuackyTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = QuackyTextPrimary
            )
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = QuackyTextSecondary
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = QuackyTextTertiary
            )
        )
    }
}

@Composable
private fun CustomCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                checkmarkColor = Color.Black,
                uncheckedColor = QuackyTextTertiary
            ),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = QuackyTextPrimary
        )
    }
}
