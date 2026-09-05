package com.khatibstudio.noyza.ui.screens.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.ui.theme.*

/**
 * Live or historical noise graph drawn on Canvas.
 * - In live mode: spans 100% of canvas width from frame 1 by pre-padding baseline readings.
 *   Points scroll continuously from right to left, with the active pulse beacon at the leading right edge.
 * - In summary mode (isLive = false): maps the recorded session samples across the full width.
 */
@Composable
fun SessionLiveGraph(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    minDb: Float = 20f,
    maxDb: Float = 100f,
    isLive: Boolean = true
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val spikeColor = LoudOrangeLight

    // Subtle radar pulse for active leading edge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val maxPoints = 120

        // Prepare points list:
        // In live mode, guarantee the curve fills 100% of the screen width from millisecond 0.
        // If we don't have maxPoints yet, pre-pad on the left with the baseline so the active
        // reading is anchored on the right edge and ripples to the left.
        val displaySamples: List<Float> = when {
            isLive -> {
                if (samples.isEmpty()) {
                    List(maxPoints) { minDb }
                } else if (samples.size < maxPoints) {
                    val baseline = samples.first()
                    List(maxPoints - samples.size) { baseline } + samples
                } else {
                    samples.takeLast(maxPoints)
                }
            }
            else -> {
                if (samples.isEmpty()) {
                    List(2) { minDb }
                } else if (samples.size == 1) {
                    listOf(samples[0], samples[0])
                } else {
                    samples
                }
            }
        }

        val pointCount = displaySamples.size
        val stepX = w / (pointCount - 1).coerceAtLeast(1).toFloat()

        // Dynamic visual range: ensure at least 28 dB window so quiet sessions have visible amplitude
        val sampleMin = displaySamples.minOrNull() ?: 20f
        val sampleMax = displaySamples.maxOrNull() ?: 50f
        val effectiveMin = minOf(minDb, sampleMin - 4f)
        val effectiveMax = maxOf(effectiveMin + 28f, sampleMax + 6f)
        val dbRange = (effectiveMax - effectiveMin).coerceAtLeast(10f)

        // Build path
        val path = Path()
        val fillPath = Path()

        var lastX = 0f
        var lastY = h

        displaySamples.forEachIndexed { index, db ->
            val x = index * stepX
            val normalizedDb = ((db - effectiveMin) / dbRange).coerceIn(0f, 1f)
            val y = h - (normalizedDb * h * 0.85f) - (h * 0.08f)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevNorm = ((displaySamples[index - 1] - effectiveMin) / dbRange).coerceIn(0f, 1f)
                val prevY = h - (prevNorm * h * 0.85f) - (h * 0.08f)

                val cp1x = prevX + stepX / 2.5f
                val cp2x = x - stepX / 2.5f

                path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
            }

            lastX = x
            lastY = y
        }

        // Close fill path along canvas bottom
        fillPath.lineTo(lastX, h)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.03f)
                )
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Active pulse beacon at current reading (right edge in live mode)
        if (isLive || samples.isNotEmpty()) {
            drawCircle(
                color = lineColor.copy(alpha = pulseAlpha),
                radius = pulseRadius * density,
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }

        // Draw spike dots for high noise points (>= 75 dB)
        displaySamples.forEachIndexed { index, db ->
            if (db >= 75f) {
                val x = index * stepX
                val normalizedDb = ((db - effectiveMin) / dbRange).coerceIn(0f, 1f)
                val y = h - (normalizedDb * h * 0.85f) - (h * 0.08f)
                drawCircle(
                    color = spikeColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Reference line at 65 dB (moderate threshold), if within view
        val refDb = 65f
        if (refDb in effectiveMin..effectiveMax) {
            val refNorm = ((refDb - effectiveMin) / dbRange).coerceIn(0f, 1f)
            val refY = h - (refNorm * h * 0.85f) - (h * 0.08f)
            drawLine(
                color = ModerateAmber.copy(alpha = 0.4f),
                start = Offset(0f, refY),
                end = Offset(w, refY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }
    }
}
