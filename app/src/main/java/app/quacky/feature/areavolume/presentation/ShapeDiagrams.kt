package app.quacky.feature.areavolume.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary
import app.quacky.feature.areavolume.domain.Point2D

@Composable
fun ShapeDiagram(
    shapeKey: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(QuackySurface)
            .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2

            when (shapeKey) {
                "rectangle" -> {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - 70f, cy - 35f),
                        size = Size(140f, 70f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(2.5f)
                    )
                }
                "square" -> {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - 40f, cy - 40f),
                        size = Size(80f, 80f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(2.5f)
                    )
                }
                "triangle" -> {
                    val path = Path().apply {
                        moveTo(cx, cy - 45f)
                        lineTo(cx - 60f, cy + 35f)
                        lineTo(cx + 60f, cy + 35f)
                        close()
                    }
                    drawPath(path, color = Color.White, style = Stroke(2.5f))
                    // Dotted height line
                    drawLine(
                        color = QuackyTextTertiary,
                        start = Offset(cx, cy - 45f),
                        end = Offset(cx, cy + 35f),
                        strokeWidth = 1.5f
                    )
                }
                "circle" -> {
                    drawCircle(
                        color = Color.White,
                        radius = 45f,
                        center = Offset(cx, cy),
                        style = Stroke(2.5f)
                    )
                    // Radius line
                    drawLine(
                        color = QuackyAccent,
                        start = Offset(cx, cy),
                        end = Offset(cx + 45f, cy),
                        strokeWidth = 2f
                    )
                    drawCircle(QuackyAccent, radius = 3.5f, center = Offset(cx, cy))
                }
                "semicircle" -> {
                    val path = Path().apply {
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(cx - 45f, cy - 45f, cx + 45f, cy + 45f),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(path, color = Color.White, style = Stroke(2.5f))
                }
                "trapezoid" -> {
                    val path = Path().apply {
                        moveTo(cx - 35f, cy - 35f)
                        lineTo(cx + 35f, cy - 35f)
                        lineTo(cx + 65f, cy + 35f)
                        lineTo(cx - 65f, cy + 35f)
                        close()
                    }
                    drawPath(path, color = Color.White, style = Stroke(2.5f))
                }
                "parallelogram" -> {
                    val path = Path().apply {
                        moveTo(cx - 30f, cy - 35f)
                        lineTo(cx + 60f, cy - 35f)
                        lineTo(cx + 30f, cy + 35f)
                        lineTo(cx - 60f, cy + 35f)
                        close()
                    }
                    drawPath(path, color = Color.White, style = Stroke(2.5f))
                }
                "ellipse" -> {
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(cx - 65f, cy - 35f),
                        size = Size(130f, 70f),
                        style = Stroke(2.5f)
                    )
                }
                "ring" -> {
                    drawCircle(color = Color.White, radius = 50f, center = Offset(cx, cy), style = Stroke(2.5f))
                    drawCircle(color = QuackyTextTertiary, radius = 25f, center = Offset(cx, cy), style = Stroke(2f))
                }
                "cube" -> {
                    // Front square
                    drawRect(color = Color.White, topLeft = Offset(cx - 40f, cy - 20f), size = Size(60f, 60f), style = Stroke(2.5f))
                    // Back square
                    drawRect(color = QuackyTextTertiary, topLeft = Offset(cx - 20f, cy - 40f), size = Size(60f, 60f), style = Stroke(1.5f))
                    // Connecting lines
                    drawLine(Color.White, Offset(cx - 40f, cy - 20f), Offset(cx - 20f, cy - 40f), 2f)
                    drawLine(Color.White, Offset(cx + 20f, cy - 20f), Offset(cx + 40f, cy - 40f), 2f)
                    drawLine(Color.White, Offset(cx + 20f, cy + 40f), Offset(cx + 40f, cy + 20f), 2f)
                    drawLine(Color.White, Offset(cx - 40f, cy + 40f), Offset(cx - 20f, cy + 20f), 2f)
                }
                "cylinder" -> {
                    drawOval(color = Color.White, topLeft = Offset(cx - 40f, cy - 45f), size = Size(80f, 20f), style = Stroke(2.5f))
                    drawOval(color = Color.White, topLeft = Offset(cx - 40f, cy + 25f), size = Size(80f, 20f), style = Stroke(2.5f))
                    drawLine(Color.White, Offset(cx - 40f, cy - 35f), Offset(cx - 40f, cy + 35f), 2.5f)
                    drawLine(Color.White, Offset(cx + 40f, cy - 35f), Offset(cx + 40f, cy + 35f), 2.5f)
                }
                "sphere" -> {
                    drawCircle(color = Color.White, radius = 45f, center = Offset(cx, cy), style = Stroke(2.5f))
                    drawOval(color = QuackyTextTertiary, topLeft = Offset(cx - 45f, cy - 15f), size = Size(90f, 30f), style = Stroke(1.5f))
                }
                "cone" -> {
                    drawOval(color = Color.White, topLeft = Offset(cx - 40f, cy + 25f), size = Size(80f, 20f), style = Stroke(2.5f))
                    drawLine(Color.White, Offset(cx, cy - 45f), Offset(cx - 40f, cy + 35f), 2.5f)
                    drawLine(Color.White, Offset(cx, cy - 45f), Offset(cx + 40f, cy + 35f), 2.5f)
                }
                else -> {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(cx - 50f, cy - 30f),
                        size = Size(100f, 60f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(2f)
                    )
                }
            }
        }
    }
}

@Composable
fun IrregularPolygonCanvas(
    points: List<Point2D>,
    onAddPoint: (Point2D) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(QuackySurface)
            .border(1.dp, QuackyOutline, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onAddPoint(Point2D(offset.x.toDouble(), offset.y.toDouble()))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (points.isNotEmpty()) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val off = Offset(p.x.toFloat(), p.y.toFloat())
                    if (i == 0) path.moveTo(off.x, off.y) else path.lineTo(off.x, off.y)
                    // Draw point dot
                    drawCircle(Color.White, radius = 5f, center = off)
                }
                if (points.size >= 3) {
                    path.close()
                    drawPath(path, color = Color.White, style = Stroke(2f))
                } else if (points.size == 2) {
                    drawLine(Color.White, Offset(points[0].x.toFloat(), points[0].y.toFloat()), Offset(points[1].x.toFloat(), points[1].y.toFloat()), 2f)
                }
            }
        }
    }
}
