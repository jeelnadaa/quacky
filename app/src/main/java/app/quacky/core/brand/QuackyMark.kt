package app.quacky.core.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyTextPrimary

/**
 * QuackyMark: Minimal geometric duck mascot rendered via vector Canvas.
 * Usable across Splash, About screen, empty states, and Easter egg animations.
 */
@Composable
fun QuackyMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = QuackyTextPrimary,
    backgroundColor: Color = QuackyBackground,
    blinkProgress: Float = 0f // 0f = eye fully open, 1f = eye closed
) {
    Canvas(modifier = modifier.size(size)) {
        val scaleFactor = this.size.width / 512f
        scale(scaleFactor, pivot = Offset.Zero) {
            drawDuck(tint, backgroundColor, blinkProgress)
        }
    }
}

private fun DrawScope.drawDuck(
    tint: Color,
    backgroundColor: Color,
    blinkProgress: Float
) {
    val bodyPath = Path().apply {
        moveTo(120f, 280f)
        cubicTo(100f, 245f, 140f, 220f, 180f, 235f)
        cubicTo(215f, 248f, 245f, 220f, 275f, 180f)
        cubicTo(290f, 160f, 305f, 130f, 340f, 130f)
        cubicTo(385f, 130f, 415f, 165f, 415f, 205f)
        cubicTo(415f, 210f, 414f, 216f, 412f, 222f)
        lineTo(470f, 236f)
        cubicTo(478f, 238f, 480f, 248f, 474f, 254f)
        lineTo(412f, 282f)
        cubicTo(395f, 330f, 345f, 365f, 290f, 370f)
        cubicTo(210f, 375f, 135f, 340f, 120f, 280f)
        close()
    }

    // Draw duck body
    drawPath(path = bodyPath, color = tint)

    // Draw beak seam
    drawLine(
        color = backgroundColor,
        start = Offset(412f, 248f),
        end = Offset(460f, 245f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    // Eye cutout / blink
    val eyeRadius = 14f
    val currentRadiusY = eyeRadius * (1f - blinkProgress.coerceIn(0f, 1f))
    if (currentRadiusY > 2f) {
        // Eye cutout
        drawOval(
            color = backgroundColor,
            topLeft = Offset(360f - eyeRadius, 190f - currentRadiusY),
            size = androidx.compose.ui.geometry.Size(eyeRadius * 2, currentRadiusY * 2)
        )
        // Catchlight when mostly open
        if (blinkProgress < 0.3f) {
            drawCircle(
                color = tint,
                radius = 4.5f,
                center = Offset(364f, 186f)
            )
        }
    } else {
        // Closed eye line for blink
        drawLine(
            color = backgroundColor,
            start = Offset(360f - eyeRadius, 190f),
            end = Offset(360f + eyeRadius, 190f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}
