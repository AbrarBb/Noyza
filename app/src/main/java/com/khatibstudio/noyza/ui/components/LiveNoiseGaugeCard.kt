package com.khatibstudio.noyza.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khatibstudio.noyza.domain.model.NoiseLevel
import com.khatibstudio.noyza.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Large animated circular gauge displaying the live noise level.
 * Smoothly animates to prevent jarring jumps.
 * Color changes with noise level — always accompanied by text (accessibility).
 */
@Composable
fun LiveNoiseGaugeCard(
    currentDb: Float,
    noiseLevel: NoiseLevel,
    averageDb: Float,
    peakDb: Float,
    durationSeconds: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate the gauge value smoothly
    val animatedDb by animateFloatAsState(
        targetValue = currentDb.coerceIn(20f, 110f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "gauge_db"
    )

    val gaugeColor = noiseLevelColor(noiseLevel)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gauge
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularGauge(
                    dbValue = animatedDb,
                    gaugeColor = gaugeColor,
                    size = 220.dp
                )

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Large dB display
                    Text(
                        text = if (isActive) "${animatedDb.toInt()}" else "--",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = gaugeColor,
                            fontSize = 52.sp
                        )
                    )
                    Text(
                        text = if (isActive) "Estimated dB" else "Not measuring",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(Modifier.height(4.dp))

                    // Noise level badge (text + color)
                    NoiseLevelBadge(noiseLevel = noiseLevel, isActive = isActive)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Average",
                    value = if (isActive && averageDb > 0) "${averageDb.toInt()} dB" else "--"
                )
                StatDivider()
                StatItem(
                    label = "Peak",
                    value = if (isActive && peakDb > 0) "${peakDb.toInt()} dB" else "--"
                )
                StatDivider()
                StatItem(
                    label = "Duration",
                    value = if (isActive) formatDuration(durationSeconds) else "--"
                )
            }

            Spacer(Modifier.height(12.dp))

            // Disclaimer with info icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Information",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Estimated microphone reading",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

@Composable
private fun CircularGauge(
    dbValue: Float,
    gaugeColor: Color,
    size: Dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.065f
        val radius = (size.toPx() / 2f) - strokeWidth
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

        // Start at 135° (bottom-left), sweep 270°
        val startAngle = 135f
        val totalSweep = 270f

        // Normalize dB: 20dB = 0%, 110dB = 100%
        val normalized = ((dbValue - 20f) / 90f).coerceIn(0f, 1f)
        val sweepAngle = normalized * totalSweep

        // Background track
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = totalSweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Active arc
        if (sweepAngle > 0f) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        gaugeColor.copy(alpha = 0.7f),
                        gaugeColor
                    ),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Range markers
        val markerDbValues = listOf(20f, 40f, 60f, 80f, 100f)
        markerDbValues.forEach { markerDb ->
            val markerNorm = ((markerDb - 20f) / 90f).coerceIn(0f, 1f)
            val markerAngle = startAngle + (markerNorm * totalSweep)
            val angleRad = Math.toRadians(markerAngle.toDouble())
            val outerRadius = radius + strokeWidth / 2f + 6.dp.toPx()
            val innerRadius = radius - strokeWidth / 2f - 2.dp.toPx()

            val outerX = center.x + (outerRadius * cos(angleRad)).toFloat()
            val outerY = center.y + (outerRadius * sin(angleRad)).toFloat()
            val innerX = center.x + (innerRadius * cos(angleRad)).toFloat()
            val innerY = center.y + (innerRadius * sin(angleRad)).toFloat()

            drawLine(
                color = trackColor,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun NoiseLevelBadge(noiseLevel: NoiseLevel, isActive: Boolean) {
    val (color, containerColor) = noiseLevelColorPair(noiseLevel)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) containerColor else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = if (isActive) noiseLevel.label else "Inactive",
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

fun noiseLevelColor(noiseLevel: NoiseLevel): Color = when (noiseLevel) {
    NoiseLevel.QUIET -> QuietGreenLight
    NoiseLevel.MODERATELY_QUIET -> QuietGreenLight
    NoiseLevel.MODERATE -> ModerateAmberLight
    NoiseLevel.LOUD -> LoudOrangeLight
    NoiseLevel.VERY_LOUD -> VeryLoudRedLight
}

fun noiseLevelColorPair(noiseLevel: NoiseLevel): Pair<Color, Color> = when (noiseLevel) {
    NoiseLevel.QUIET, NoiseLevel.MODERATELY_QUIET ->
        Pair(QuietGreen, QuietGreenContainer)
    NoiseLevel.MODERATE ->
        Pair(ModerateAmber, ModerateAmberContainer)
    NoiseLevel.LOUD ->
        Pair(LoudOrange, LoudOrangeContainer)
    NoiseLevel.VERY_LOUD ->
        Pair(VeryLoudRed, VeryLoudRedContainer)
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}
