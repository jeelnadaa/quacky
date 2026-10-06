package app.quacky.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.CardCornerRadius
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.SatoshiFontFamily
import app.quacky.core.registry.ToolCategory
import app.quacky.core.registry.ToolRegistry

@Composable
fun CategoryRow(
    category: ToolCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count = ToolRegistry.getByCategory(category).size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CardCornerRadius),
        color = QuackySurface,
        border = BorderStroke(1.dp, QuackyOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = QuackyTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(category.titleRes),
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = QuackyTextPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = QuackyTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = QuackyTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
