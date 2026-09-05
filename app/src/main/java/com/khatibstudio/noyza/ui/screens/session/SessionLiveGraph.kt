package com.khatibstudio.noyza.ui.screens.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.ui.theme.*

/**
 * Live noise graph drawn on Canvas.
 * Renders a smooth line graph of dB samples.
 * Fills area under the curve with gradient.
 * Highlights spike regions in orange/red.
 */
@Composable
fun SessionLiveGraph(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    minDb: Float = 20f,
    maxDb: Float = 100f
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
        if (samples.size < 2) return@Canvas

        val w = size.width
        val h = size.height

        // Dynamic visual range: ensure at least 28 dB window so quiet sessions have visible amplitude
        val sampleMin = samples.minOrNull() ?: 20f
        val sampleMax = samples.maxOrNull() ?: 50f
        val effectiveMin = minOf(minDb, sampleMin - 4f)
        val effectiveMax = maxOf(effectiveMin + 28f, sampleMax + 6f)
        val dbRange = (effectiveMax - effectiveMin).coerceAtLeast(10f)

        // Rolling capacity so curve scrolls steadily across screen
        val maxPoints = 120
        val stepX = w / (maxPoints - 1).coerceAtLeast(1).toFloat()

        // Build path
        val path = Path()
        val fillPath = Path()

        var lastX = 0f
        var lastY = h

        val startIndex = (samples.size - maxPoints).coerceAtLeast(0)
        val visibleSamples = samples.subList(startIndex, samples.size)

        visibleSamples.forEachIndexed { index, db ->
            val x = index * stepX
            val normalizedDb = ((db - effectiveMin) / dbRange).coerceIn(0f, 1f)
            val y = h - (normalizedDb * h * 0.85f) - (h * 0.08f)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevNorm = ((visibleSamples[index - 1] - effectiveMin) / dbRange).coerceIn(0f, 1f)
                val prevY = h - (prevNorm * h * 0.85f) - (h * 0.08f)

                val cp1x = prevX + stepX / 2.5f
                val cp2x = x - stepX / 2.5f

                path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
            }

            lastX = x
            lastY = y
        }

        // Close fill path
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

        // Active pulse beacon at current reading
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

        // Draw spike dots for high noise points (>= 75 dB)
        visibleSamples.forEachIndexed { index, db ->
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
                end = Offset(lastX.coerceAtLeast(w), refY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }
    }
}
