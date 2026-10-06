package app.quacky.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 14.dp
val ButtonCornerRadius = 12.dp
val InputCornerRadius = 12.dp
val PillCornerRadius = 999.dp

val QuackyShapes = Shapes(
    small = RoundedCornerShape(ButtonCornerRadius),
    medium = RoundedCornerShape(CardCornerRadius),
    large = RoundedCornerShape(16.dp),
    extraSmall = RoundedCornerShape(8.dp),
    extraLarge = CircleShape
)
