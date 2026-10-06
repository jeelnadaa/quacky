package app.quacky.feature.compressor.presentation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.designsystem.theme.QuackyOutline
import app.quacky.core.designsystem.theme.QuackySurface
import app.quacky.core.designsystem.theme.QuackyTextPrimary
import coil.compose.AsyncImage

@Composable
fun SplitComparisonView(
    originalUri: Uri,
    compressedUri: Uri?,
    originalSizeText: String,
    compressedSizeText: String,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(QuackySurface)
            .border(1.dp, QuackyOutline, RoundedCornerShape(14.dp))
            .clipToBounds()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                    if (zoomScale > 1f) {
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    } else {
                        panOffsetX = 0f
                        panOffsetY = 0f
                    }
                }
            }
    ) {
        val imageModifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = zoomScale,
                scaleY = zoomScale,
                translationX = panOffsetX,
                translationY = panOffsetY
            )

        // Bottom layer: Compressed image (or original if not compressed yet)
        AsyncImage(
            model = compressedUri ?: originalUri,
            contentDescription = "Compressed Preview",
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )

        // Top layer: Original image clipped to split fraction
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = splitFraction)
                .clipToBounds()
        ) {
            AsyncImage(
                model = originalUri,
                contentDescription = "Original Preview",
                contentScale = ContentScale.Fit,
                modifier = imageModifier.fillMaxWidth(1f / splitFraction.coerceAtLeast(0.01f))
            )
        }

        // Draggable vertical split line
        val linePositionPx = widthPx * splitFraction
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset { IntOffset(linePositionPx.toInt() - 1, 0) }
                .background(Color.White)
        )

        // Drag handle pill in center
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(linePositionPx.toInt() - 14, 0) }
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, QuackyOutline, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        splitFraction = ((linePositionPx + dragAmount.x) / widthPx).coerceIn(0.05f, 0.95f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↔",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Top Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Original badge (Left)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(QuackyBackground.copy(alpha = 0.85f))
                    .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Original: $originalSizeText",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = QuackyTextPrimary
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            // Compressed badge (Right)
            if (compressedUri != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(QuackyBackground.copy(alpha = 0.85f))
                    .border(1.dp, QuackyOutline, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Compressed: $compressedSizeText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = QuackyTextPrimary
                    )
                }
            }
        }
    }
}
