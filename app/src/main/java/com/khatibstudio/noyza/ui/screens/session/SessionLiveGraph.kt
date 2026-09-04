package com.khatibstudio.noyza.ui.screens.session

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

    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val dbRange = maxDb - minDb
        val stepX = w / (samples.size - 1).toFloat()

        // Build path
        val path = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, db ->
            val x = index * stepX
            val normalizedDb = ((db - minDb) / dbRange).coerceIn(0f, 1f)
            val y = h - (normalizedDb * h * 0.9f) - (h * 0.05f)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                // Smooth cubic bezier between points
                val prevX = (index - 1) * stepX
                val prevNorm = ((samples[index - 1] - minDb) / dbRange).coerceIn(0f, 1f)
                val prevY = h - (prevNorm * h * 0.9f) - (h * 0.05f)

                val cp1x = prevX + stepX / 3f
                val cp2x = x - stepX / 3f

                path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
            }
        }

        // Close fill path
        fillPath.lineTo(w, h)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.3f),
                    lineColor.copy(alpha = 0.05f)
                )
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw spike dots for high noise points
        samples.forEachIndexed { index, db ->
            if (db >= 75f) {
                val x = index * stepX
                val normalizedDb = ((db - minDb) / dbRange).coerceIn(0f, 1f)
                val y = h - (normalizedDb * h * 0.9f) - (h * 0.05f)
                drawCircle(
                    color = spikeColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Reference line at 65 dB (moderate threshold)
        val refDb = 65f
        val refNorm = ((refDb - minDb) / dbRange).coerceIn(0f, 1f)
        val refY = h - (refNorm * h * 0.9f) - (h * 0.05f)
        drawLine(
            color = ModerateAmber.copy(alpha = 0.4f),
            start = Offset(0f, refY),
            end = Offset(w, refY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    }
}
