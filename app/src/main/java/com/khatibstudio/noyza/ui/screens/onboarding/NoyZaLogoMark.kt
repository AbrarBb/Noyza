package com.khatibstudio.noyza.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.ui.theme.White

/**
 * Noyza logo mark — minimalist sound wave with N letterform.
 * Works at small sizes (notification icon) through large (splash screen).
 */
@Composable
fun NoyZaLogoMark(
    size: Dp = 48.dp,
    color: Color = White
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val cx = w / 2f
        val cy = h / 2f
        val strokeWidth = w * 0.06f

        // Draw sound waves (concentric arcs on right side)
        val waveColor = color.copy(alpha = 0.9f)
        val wave1Radius = w * 0.18f
        val wave2Radius = w * 0.30f
        val wave3Radius = w * 0.42f

        listOf(wave1Radius, wave2Radius, wave3Radius).forEachIndexed { index, radius ->
            val alpha = 1f - (index * 0.2f)
            drawArc(
                color = waveColor.copy(alpha = alpha),
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center dot (location pin concept)
        drawCircle(
            color = color,
            radius = w * 0.07f,
            center = Offset(cx * 0.7f, cy)
        )
    }
}
