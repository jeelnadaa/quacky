package app.quacky.core.tips

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.quacky.core.designsystem.theme.QuackyAccent
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import app.quacky.core.designsystem.theme.QuackyTextSecondary
import app.quacky.core.designsystem.theme.QuackyTextTertiary

@Composable
fun TipVisual(
    type: VisualType,
    a11yDesc: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "tip_anim")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .semantics { contentDescription = a11yDesc }
    ) {
        // Background card
        drawRoundRect(
            color = QuackySurface,
            size = size,
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = QuackyOutline,
            size = size,
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 2f)
        )

        // Render according to visual type
        when (type) {
            VisualType.SCANNER_SWEEP -> drawScannerVisual(progress)
            VisualType.PINCH_ZOOM -> drawPinchZoomVisual(progress)
            VisualType.GALLERY_PICK -> drawGalleryPickVisual(progress)
            VisualType.ACTION_SHEET -> drawActionSheetVisual(progress)
            VisualType.LOCK_URL -> drawLockUrlVisual(progress)
            VisualType.CHIP_SELECT -> drawChipSelectVisual(progress)
            VisualType.LIVE_TYPE -> drawLiveTypeVisual(progress)
            VisualType.RULER_ALIGN -> drawRulerAlignVisual(progress)
            VisualType.RULER_CALIBRATE -> drawRulerCalibrateVisual(progress)
            VisualType.RULER_MARKERS -> drawRulerMarkersVisual(progress)
            VisualType.RULER_FLIP -> drawRulerFlipVisual(progress)
            VisualType.COLOR_RETICLE -> drawColorReticleVisual(progress)
            VisualType.COLOR_NUDGE -> drawColorNudgeVisual(progress)
            VisualType.DICE_ROLL -> drawDiceRollVisual(progress)
            VisualType.COIN_FLIP -> drawCoinFlipVisual(progress)
            VisualType.WHEEL_SPIN -> drawWheelSpinVisual(progress)
            VisualType.TEAM_SPLIT -> drawTeamSplitVisual(progress)
            VisualType.METADATA_PICK -> drawMetadataPickVisual(progress)
            VisualType.METADATA_INSPECT -> drawMetadataInspectVisual(progress)
            VisualType.METADATA_REMOVE -> drawMetadataRemoveVisual(progress)
            VisualType.METADATA_CLEAN -> drawMetadataCleanVisual(progress)
            VisualType.COMPRESS_PICK -> drawCompressPickVisual(progress)
            VisualType.COMPRESS_SLIDER -> drawCompressSliderVisual(progress)
            VisualType.COMPRESS_SPLIT -> drawCompressSplitVisual(progress)
            VisualType.COMPRESS_SAVE -> drawCompressSaveVisual(progress)
            VisualType.AREA_TABS -> drawAreaTabsVisual(progress)
            VisualType.AREA_SHAPE -> drawAreaShapeVisual(progress)
            VisualType.AREA_RESULTS -> drawAreaResultsVisual(progress)
            VisualType.AREA_ESTIMATE -> drawAreaEstimateVisual(progress)
            VisualType.AR_PLANE -> drawArPlaneVisual(progress)
            VisualType.AR_POINTS -> drawArPointsVisual(progress)
            VisualType.AR_MODES -> drawArModesVisual(progress)
            VisualType.AR_HEIGHT -> drawArHeightVisual(progress)
            VisualType.AR_ESTIMATE -> drawArEstimateVisual(progress)
            else -> drawGenericTipVisual(progress)
        }
    }
}

private fun DrawScope.drawPhoneOutline(centerX: Float, centerY: Float, width: Float, height: Float) {
    drawRoundRect(
        color = QuackyTextTertiary,
        topLeft = Offset(centerX - width / 2, centerY - height / 2),
        size = Size(width, height),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawFingerTap(x: Float, y: Float, pulseProgress: Float) {
    val radius = 10f + pulseProgress * 24f
    val alpha = (1f - pulseProgress).coerceIn(0f, 1f)
    drawCircle(
        color = QuackyAccent.copy(alpha = alpha),
        radius = radius,
        center = Offset(x, y),
        style = Stroke(width = 2f)
    )
    drawCircle(
        color = QuackyAccent,
        radius = 8f,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawScannerVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val frameSize = 100f

    // Viewfinder brackets
    val left = cx - frameSize / 2
    val right = cx + frameSize / 2
    val top = cy - frameSize / 2
    val bottom = cy + frameSize / 2
    val bracketLen = 20f

    // Top-left
    drawLine(QuackyTextPrimary, Offset(left, top), Offset(left + bracketLen, top), 4f)
    drawLine(QuackyTextPrimary, Offset(left, top), Offset(left, top + bracketLen), 4f)
    // Top-right
    drawLine(QuackyTextPrimary, Offset(right, top), Offset(right - bracketLen, top), 4f)
    drawLine(QuackyTextPrimary, Offset(right, top), Offset(right, top + bracketLen), 4f)
    // Bottom-left
    drawLine(QuackyTextPrimary, Offset(left, bottom), Offset(left + bracketLen, bottom), 4f)
    drawLine(QuackyTextPrimary, Offset(left, bottom), Offset(left, bottom - bracketLen), 4f)
    // Bottom-right
    drawLine(QuackyTextPrimary, Offset(right, bottom), Offset(right - bracketLen, bottom), 4f)
    drawLine(QuackyTextPrimary, Offset(right, bottom), Offset(right, bottom - bracketLen), 4f)

    // Moving scanline
    val lineY = top + (bottom - top) * progress
    drawLine(
        color = QuackyAccent,
        start = Offset(left + 6f, lineY),
        end = Offset(right - 6f, lineY),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawPinchZoomVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val spread = progress * 40f

    // Two finger dots moving outward
    drawCircle(QuackyTextPrimary, radius = 10f, center = Offset(cx - 20f - spread, cy - 20f - spread))
    drawCircle(QuackyTextPrimary, radius = 10f, center = Offset(cx + 20f + spread, cy + 20f + spread))

    // Expanding square in center
    val sz = 40f + spread * 1.5f
    drawRoundRect(
        color = QuackyTextSecondary,
        topLeft = Offset(cx - sz / 2, cy - sz / 2),
        size = Size(sz, sz),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawGalleryPickVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Photo card sliding in
    val cardX = cx - 50f + (progress * 20f)
    drawRoundRect(
        color = QuackyTextSecondary,
        topLeft = Offset(cardX, cy - 40f),
        size = Size(80f, 80f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 2f)
    )
    drawFingerTap(cx + 10f, cy + 10f, (progress * 2f) % 1f)
}

private fun DrawScope.drawActionSheetVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Action items
    for (i in 0..2) {
        val y = cy - 30f + i * 24f
        val isHighlighted = ((progress * 3).toInt() % 3) == i
        drawRoundRect(
            color = if (isHighlighted) QuackyAccent else QuackyOutline,
            topLeft = Offset(cx - 60f, y),
            size = Size(120f, 18f),
            cornerRadius = CornerRadius(6f, 6f),
            style = if (isHighlighted) Stroke(2f) else Stroke(1f)
        )
    }
}

private fun DrawScope.drawLockUrlVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // URL bar with lock icon
    drawRoundRect(
        color = QuackyOutline,
        topLeft = Offset(cx - 80f, cy - 16f),
        size = Size(160f, 32f),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(2f)
    )
    // Small lock circle
    drawCircle(QuackyAccent, radius = 6f, center = Offset(cx - 60f, cy))
}

private fun DrawScope.drawChipSelectVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val activeIdx = ((progress * 3).toInt() % 3)

    for (i in 0..2) {
        val x = cx - 75f + i * 55f
        val isSelected = i == activeIdx
        drawRoundRect(
            color = if (isSelected) QuackyAccent else QuackyTextTertiary,
            topLeft = Offset(x, cy - 12f),
            size = Size(46f, 24f),
            cornerRadius = CornerRadius(12f, 12f),
            style = Stroke(2f)
        )
    }
}

private fun DrawScope.drawLiveTypeVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Typing text cursor
    val cursorX = cx - 50f + (progress * 70f)
    drawLine(QuackyTextPrimary, Offset(cx - 50f, cy), Offset(cursorX, cy), 3f)
    if ((progress * 6).toInt() % 2 == 0) {
        drawLine(QuackyAccent, Offset(cursorX + 4f, cy - 10f), Offset(cursorX + 4f, cy + 10f), 3f)
    }
}

private fun DrawScope.drawRulerAlignVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Ruler edge markings
    for (i in 0..10) {
        val x = cx - 80f + i * 16f
        val h = if (i % 5 == 0) 24f else 12f
        drawLine(QuackyTextPrimary, Offset(x, cy + 30f), Offset(x, cy + 30f - h), 2f)
    }

    // Object being aligned
    val objX = cx - 80f + progress * 20f
    drawRoundRect(
        color = QuackyTextSecondary,
        topLeft = Offset(objX, cy - 20f),
        size = Size(60f, 30f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(2f)
    )
}

private fun DrawScope.drawRulerCalibrateVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val scaleFactor = 0.9f + progress * 0.2f

    // Credit card outline scaling
    val w = 110f * scaleFactor
    val h = 70f * scaleFactor
    drawRoundRect(
        color = QuackyAccent,
        topLeft = Offset(cx - w / 2, cy - h / 2),
        size = Size(w, h),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(2f)
    )
}

private fun DrawScope.drawRulerMarkersVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val markerGap = 30f + progress * 60f

    // Marker 1
    drawLine(QuackyAccent, Offset(cx - markerGap / 2, cy - 35f), Offset(cx - markerGap / 2, cy + 35f), 3f)
    // Marker 2
    drawLine(QuackyAccent, Offset(cx + markerGap / 2, cy - 35f), Offset(cx + markerGap / 2, cy + 35f), 3f)
    // Distance line
    drawLine(QuackyTextSecondary, Offset(cx - markerGap / 2, cy), Offset(cx + markerGap / 2, cy), 1.5f)
}

private fun DrawScope.drawRulerFlipVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val flip = progress > 0.5f
    val edgeX = if (flip) cx + 45f else cx - 45f
    // Vertical line with tick marks
    drawLine(QuackyTextPrimary, Offset(edgeX, cy - 40f), Offset(edgeX, cy + 40f), 2f)
    for (i in -4..4) {
        val y = cy + i * 10f
        val len = if (i % 2 == 0) 14f else 7f
        val startX = edgeX
        val endX = if (flip) edgeX - len else edgeX + len
        drawLine(QuackyTextSecondary, Offset(startX, y), Offset(endX, y), 1.5f)
    }
}

private fun DrawScope.drawColorReticleVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val loupeY = cy - 35f

    // Reticle crosshair
    drawLine(QuackyTextPrimary, Offset(cx - 20f, cy + 20f), Offset(cx + 20f, cy + 20f), 2f)
    drawLine(QuackyTextPrimary, Offset(cx, cy), Offset(cx, cy + 40f), 2f)

    // Loupe above finger
    drawCircle(QuackyAccent, radius = 24f, center = Offset(cx, loupeY), style = Stroke(2f))
    drawCircle(QuackyTextSecondary, radius = 6f, center = Offset(cx, loupeY), style = Stroke(1.5f))
}

private fun DrawScope.drawColorNudgeVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val step = ((progress * 4).toInt()) * 6f

    // Crosshair nudging by 1 pixel increments
    drawCircle(QuackyAccent, radius = 8f, center = Offset(cx + step, cy), style = Stroke(2f))
    // Arrow pointing
    drawLine(QuackyTextSecondary, Offset(cx + 30f, cy), Offset(cx + 50f, cy), 2f)
}

private fun DrawScope.drawDiceRollVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Die outline
    drawRoundRect(
        color = QuackyTextPrimary,
        topLeft = Offset(cx - 25f, cy - 25f),
        size = Size(50f, 50f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(3f)
    )
    // Pips or number changing
    val pips = ((progress * 6).toInt() % 6) + 1
    drawCircle(QuackyAccent, radius = 4f, center = Offset(cx, cy))
}

private fun DrawScope.drawCoinFlipVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val scaleX = kotlin.math.cos(progress * kotlin.math.PI * 2).toFloat()

    drawOval(
        color = QuackyTextPrimary,
        topLeft = Offset(cx - 28f * kotlin.math.abs(scaleX), cy - 28f),
        size = Size(56f * kotlin.math.abs(scaleX), 56f),
        style = Stroke(3f)
    )
}

private fun DrawScope.drawWheelSpinVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    val radius = 40f

    drawCircle(QuackyOutline, radius = radius, center = Offset(cx, cy), style = Stroke(2f))
    // Spinning pointer
    val angle = progress * 360f
    val rad = Math.toRadians(angle.toDouble())
    val px = cx + (radius * Math.cos(rad)).toFloat()
    val py = cy + (radius * Math.sin(rad)).toFloat()
    drawLine(QuackyAccent, Offset(cx, cy), Offset(px, py), 2.5f)
}

private fun DrawScope.drawTeamSplitVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Team 1 box
    drawRoundRect(QuackyTextSecondary, Offset(cx - 70f, cy - 25f), Size(60f, 50f), CornerRadius(6f, 6f), Stroke(2f))
    // Team 2 box
    drawRoundRect(QuackyTextSecondary, Offset(cx + 10f, cy - 25f), Size(60f, 50f), CornerRadius(6f, 6f), Stroke(2f))
}

private fun DrawScope.drawGenericTipVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    drawCircle(QuackyAccent, radius = 20f + progress * 10f, center = Offset(cx, cy), style = Stroke(2f))
}

private fun DrawScope.drawMetadataPickVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Grid of 3 photo cards
    for (i in 0..2) {
        val x = cx - 75f + (i * 55f)
        drawRoundRect(QuackyTextTertiary, Offset(x, cy - 25f), Size(45f, 50f), CornerRadius(6f, 6f), Stroke(2f))
        if (i < 2 || progress > 0.5f) {
            // Checkmark
            drawCircle(QuackyAccent, radius = 6f, center = Offset(x + 35f, cy - 15f))
        }
    }
}

private fun DrawScope.drawMetadataInspectVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Photo on left
    drawRoundRect(QuackyTextSecondary, Offset(cx - 65f, cy - 30f), Size(55f, 60f), CornerRadius(6f, 6f), Stroke(2f))
    // Data tags unfolding on right
    val tagAnim = (progress * 3).toInt().coerceIn(0, 2)
    val tags = listOf("GPS 37°N", "ISO 100", "Pixel 8")
    for (i in 0..tagAnim) {
        val y = cy - 25f + (i * 20f)
        drawRoundRect(QuackyOutline, Offset(cx + 5f, y), Size(65f, 16f), CornerRadius(4f, 4f))
        drawLine(QuackyTextPrimary, Offset(cx + 12f, y + 8f), Offset(cx + 45f, y + 8f), 2f)
    }
}

private fun DrawScope.drawMetadataRemoveVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // 3 chips: All, Location, Custom
    val chipNames = listOf("All", "Location", "Custom")
    for (i in 0..2) {
        val x = cx - 85f + (i * 60f)
        drawRoundRect(QuackyOutline, Offset(x, cy - 14f), Size(52f, 28f), CornerRadius(14f, 14f))
        // Strikethrough line on location chip (index 1)
        if (i == 1 && progress > 0.3f) {
            drawLine(QuackyAccent, Offset(x + 8f, cy), Offset(x + 44f, cy), 2f)
        }
    }
}

private fun DrawScope.drawMetadataCleanVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Original photo
    drawRoundRect(QuackyTextTertiary, Offset(cx - 60f, cy - 25f), Size(45f, 50f), CornerRadius(6f, 6f), Stroke(2f))
    // Arrow
    drawLine(QuackyTextSecondary, Offset(cx - 5f, cy), Offset(cx + 15f, cy), 2f)
    // Clean photo with shield
    drawRoundRect(QuackyAccent, Offset(cx + 25f, cy - 25f), Size(45f, 50f), CornerRadius(6f, 6f), Stroke(2f))
    drawCircle(QuackyAccent, radius = 6f, center = Offset(cx + 47f, cy))
}

private fun DrawScope.drawCompressPickVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Image card
    val dropY = cy - 25f + ((1f - progress.coerceIn(0f, 0.4f) / 0.4f) * -30f)
    drawRoundRect(QuackyTextPrimary, Offset(cx - 55f, dropY), Size(45f, 50f), CornerRadius(6f, 6f), Stroke(2f))
    // PDF card
    val dropY2 = cy - 25f + ((1f - (progress - 0.2f).coerceIn(0f, 0.4f) / 0.4f) * -30f)
    drawRoundRect(QuackyTextSecondary, Offset(cx + 10f, dropY2), Size(45f, 50f), CornerRadius(6f, 6f), Stroke(2f))
}

private fun DrawScope.drawCompressSliderVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Slider track
    drawLine(QuackyOutline, Offset(cx - 60f, cy), Offset(cx + 60f, cy), 4f, StrokeCap.Round)
    // Thumb moving
    val thumbX = cx - 60f + (progress * 120f)
    drawLine(QuackyAccent, Offset(cx - 60f, cy), Offset(thumbX, cy), 4f, StrokeCap.Round)
    drawCircle(QuackyAccent, radius = 8f, center = Offset(thumbX, cy))
}

private fun DrawScope.drawCompressSplitVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Image frame
    drawRoundRect(QuackyOutline, Offset(cx - 60f, cy - 35f), Size(120f, 70f), CornerRadius(8f, 8f))
    // Split line oscillating
    val splitX = cx - 40f + (kotlin.math.sin(progress * kotlin.math.PI * 2).toFloat() * 35f)
    drawLine(QuackyAccent, Offset(splitX, cy - 35f), Offset(splitX, cy + 35f), 2.5f)
    drawCircle(QuackyAccent, radius = 5f, center = Offset(splitX, cy))
}

private fun DrawScope.drawCompressSaveVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Result card
    drawRoundRect(QuackySurface, Offset(cx - 65f, cy - 25f), Size(130f, 50f), CornerRadius(8f, 8f))
    drawRoundRect(QuackyOutline, Offset(cx - 65f, cy - 25f), Size(130f, 50f), CornerRadius(8f, 8f), Stroke(1.5f))
    // Percentage badge
    drawRoundRect(QuackyAccent, Offset(cx - 50f, cy - 10f), Size(50f, 20f), CornerRadius(4f, 4f))
    // Save check
    if (progress > 0.4f) {
        drawCircle(QuackyAccent, radius = 8f, center = Offset(cx + 40f, cy))
    }
}

private fun DrawScope.drawAreaTabsVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Two tab boxes
    val activeTab = if (progress < 0.5f) 0 else 1
    drawRoundRect(if (activeTab == 0) QuackyAccent else QuackySurface, Offset(cx - 70f, cy - 18f), Size(65f, 36f), CornerRadius(8f, 8f))
    drawRoundRect(if (activeTab == 1) QuackyAccent else QuackySurface, Offset(cx + 5f, cy - 18f), Size(65f, 36f), CornerRadius(8f, 8f))
}

private fun DrawScope.drawAreaShapeVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Labeled rectangle
    drawRect(QuackyOutline, Offset(cx - 50f, cy - 25f), Size(100f, 50f), style = Stroke(2f))
    // Side 'a' highlight
    val aColor = if (progress > 0.3f) QuackyAccent else QuackyTextSecondary
    drawLine(aColor, Offset(cx - 50f, cy - 25f), Offset(cx + 50f, cy - 25f), 3.5f)
}

private fun DrawScope.drawAreaResultsVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Card with expanding units
    val h = 40f + (progress * 30f)
    drawRoundRect(QuackySurface, Offset(cx - 60f, cy - h / 2), Size(120f, h), CornerRadius(8f, 8f))
    drawRoundRect(QuackyOutline, Offset(cx - 60f, cy - h / 2), Size(120f, h), CornerRadius(8f, 8f), Stroke(1.5f))
}

private fun DrawScope.drawAreaEstimateVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2
    // Wall rectangle with door cut out
    drawRect(QuackyTextSecondary, Offset(cx - 55f, cy - 30f), Size(80f, 60f), style = Stroke(2f))
    drawRect(QuackyOutline, Offset(cx - 25f, cy), Size(20f, 30f))
    // Bucket/number
    drawCircle(QuackyAccent, radius = 10f, center = Offset(cx + 45f, cy))
}

private fun DrawScope.drawArPlaneVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Swaying phone outline
    val sway = kotlin.math.sin(progress * kotlin.math.PI * 2).toFloat() * 30f
    drawPhoneOutline(cx + sway, cy - 25f, 60f, 90f)

    // Floor plane dots fading in
    val dotAlpha = (progress * 1.5f).coerceIn(0.2f, 1f)
    for (ix in -3..3) {
        for (iy in 0..2) {
            val px = cx + (ix * 28f)
            val py = cy + 30f + (iy * 18f)
            drawCircle(
                color = QuackyAccent.copy(alpha = dotAlpha),
                radius = 2.5f,
                center = Offset(px, py)
            )
        }
    }
}

private fun DrawScope.drawArPointsVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    val p1 = Offset(cx - 65f, cy)
    val p2 = Offset(cx + 65f, cy)

    // Start point
    drawCircle(QuackyAccent, radius = 6f, center = p1)
    drawCircle(QuackyTextSecondary, radius = 12f, center = p1, style = Stroke(1.5f))

    // End point appears
    if (progress > 0.4f) {
        val lineProgress = ((progress - 0.4f) / 0.4f).coerceIn(0f, 1f)
        val currX = p1.x + (p2.x - p1.x) * lineProgress
        drawLine(QuackyAccent, p1, Offset(currX, cy), strokeWidth = 3f)

        if (progress > 0.8f) {
            drawCircle(QuackyAccent, radius = 6f, center = p2)
            drawCircle(QuackyTextSecondary, radius = 12f, center = p2, style = Stroke(1.5f))

            // Distance pill at midpoint
            drawRoundRect(
                color = QuackySurface,
                topLeft = Offset(cx - 25f, cy - 14f),
                size = Size(50f, 28f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = QuackyOutline,
                topLeft = Offset(cx - 25f, cy - 14f),
                size = Size(50f, 28f),
                cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(1.5f)
            )
            drawCircle(QuackyAccent, radius = 3f, center = Offset(cx, cy))
        }
    }
}

private fun DrawScope.drawArModesVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    val activeIndex = (progress * 4).toInt().coerceIn(0, 3)
    val startX = cx - 75f

    for (i in 0..3) {
        val bx = startX + (i * 40f)
        val isSelected = (i == activeIndex)
        val bg = if (isSelected) QuackyAccent else QuackySurface
        val border = if (isSelected) QuackyAccent else QuackyOutline

        drawRoundRect(
            color = bg,
            topLeft = Offset(bx, cy - 18f),
            size = Size(32f, 36f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = border,
            topLeft = Offset(bx, cy - 18f),
            size = Size(32f, 36f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(1.5f)
        )
        // Mini icon dot or line
        val icColor = if (isSelected) QuackySurface else QuackyTextSecondary
        drawCircle(icColor, radius = 4f, center = Offset(bx + 16f, cy))
    }
}

private fun DrawScope.drawArHeightVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Floor line
    drawLine(QuackyTextTertiary, Offset(cx - 80f, cy + 45f), Offset(cx + 80f, cy + 45f), strokeWidth = 2f)

    // Floor tap ring
    val floorPoint = Offset(cx, cy + 45f)
    drawCircle(QuackyAccent, radius = 5f, center = floorPoint)
    drawCircle(QuackyTextSecondary, radius = 10f, center = floorPoint, style = Stroke(1.5f))

    // Vertical line rising up
    val heightProgress = progress.coerceIn(0f, 1f)
    val topY = floorPoint.y - (80f * heightProgress)
    drawLine(QuackyAccent, floorPoint, Offset(cx, topY), strokeWidth = 2.5f)

    if (progress > 0.6f) {
        drawCircle(QuackyAccent, radius = 5f, center = Offset(cx, topY))

        // Height pill
        drawRoundRect(
            color = QuackySurface,
            topLeft = Offset(cx + 12f, cy - 10f),
            size = Size(45f, 22f),
            cornerRadius = CornerRadius(11f, 11f)
        )
        drawRoundRect(
            color = QuackyOutline,
            topLeft = Offset(cx + 12f, cy - 10f),
            size = Size(45f, 22f),
            cornerRadius = CornerRadius(11f, 11f),
            style = Stroke(1.5f)
        )
    }
}

private fun DrawScope.drawArEstimateVisual(progress: Float) {
    val cx = size.width / 2
    val cy = size.height / 2

    // Center pill showing measurement
    drawRoundRect(
        color = QuackySurface,
        topLeft = Offset(cx - 55f, cy - 22f),
        size = Size(110f, 44f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = QuackyOutline,
        topLeft = Offset(cx - 55f, cy - 22f),
        size = Size(110f, 44f),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(1.5f)
    )

    // Estimate indicator line with +/- wobble
    val wobble = kotlin.math.sin(progress * kotlin.math.PI * 4).toFloat() * 3f
    drawLine(QuackyTextSecondary, Offset(cx - 30f + wobble, cy), Offset(cx - 15f + wobble, cy), strokeWidth = 2f)
    drawLine(QuackyAccent, Offset(cx - 5f, cy), Offset(cx + 35f, cy), strokeWidth = 3f)
}




