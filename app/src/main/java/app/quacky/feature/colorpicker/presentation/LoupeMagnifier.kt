package app.quacky.feature.colorpicker.presentation

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import kotlin.math.roundToInt

@Composable
fun LoupeMagnifier(
    bitmap: Bitmap,
    centerPxX: Int,
    centerPxY: Int,
    currentColorHex: String,
    screenOffsetX: Float,
    screenOffsetY: Float,
    modifier: Modifier = Modifier
) {
    val loupeDiameter = 120.dp
    // Place loupe offset above the finger (e.g. 80dp above finger)
    val loupeLiftPx = 220

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (screenOffsetX - 60 * 2.7f).roundToInt(),
                    (screenOffsetY - loupeLiftPx).roundToInt()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Circular 8x Pixel Grid Canvas
            Box(
                modifier = Modifier
                    .size(loupeDiameter)
                    .clip(CircleShape)
                    .background(QuackyBackground)
                    .border(2.5.dp, QuackyAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(loupeDiameter)) {
                    val gridSize = 11 // 11x11 pixel window
                    val half = gridSize / 2
                    val cellWidth = size.width / gridSize
                    val cellHeight = size.height / gridSize

                    for (dy in -half..half) {
                        for (dx in -half..half) {
                            val px = (centerPxX + dx).coerceIn(0, bitmap.width - 1)
                            val py = (centerPxY + dy).coerceIn(0, bitmap.height - 1)
                            val c = bitmap.getPixel(px, py)

                            val cellLeft = (dx + half) * cellWidth
                            val cellTop = (dy + half) * cellHeight

                            // Draw pixel block
                            drawRect(
                                color = androidx.compose.ui.graphics.Color(c),
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellWidth, cellHeight)
                            )

                            // Faint grid lines
                            drawRect(
                                color = androidx.compose.ui.graphics.Color(0x33000000),
                                topLeft = Offset(cellLeft, cellTop),
                                size = Size(cellWidth, cellHeight),
                                style = Stroke(0.5f)
                            )
                        }
                    }

                    // Highlight center target pixel
                    val centerLeft = half * cellWidth
                    val centerTop = half * cellHeight
                    drawRect(
                        color = QuackyAccent,
                        topLeft = Offset(centerLeft, centerTop),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(2.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live HEX label pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(QuackySurface)
                    .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = currentColorHex,
                    color = QuackyTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
