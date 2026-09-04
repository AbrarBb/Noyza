package com.khatibstudio.noyza.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.SuitabilityResult
import com.khatibstudio.noyza.domain.model.SuitabilityState
import com.khatibstudio.noyza.ui.theme.*

/**
 * Suitability score card — shows score/100 with circular indicator,
 * state label, and description.
 * The score counter-animates from current to new value.
 */
@Composable
fun SuitabilityScoreCard(
    result: SuitabilityResult,
    activity: ActivityType,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateIntAsState(
        targetValue = result.score,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "score_animation"
    )

    val scoreColor = scoreStateColor(result.state)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${activity.displayName} Suitability",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            // Circular score indicator
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                ScoreCircle(
                    score = animatedScore,
                    color = scoreColor
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            fontSize = 38.sp
                        )
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // State label
            Text(
                text = result.state.label,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            )

            Spacer(Modifier.height(8.dp))

            // Headline
            Text(
                text = result.headline,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = "\"${result.description}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun ScoreCircle(score: Int, color: Color) {
    val normalized = (score / 100f).coerceIn(0f, 1f)

    Canvas(modifier = Modifier.size(120.dp)) {
        val strokeWidth = 10.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Track
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Progress arc
        if (normalized > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = normalized * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

fun scoreStateColor(state: SuitabilityState): Color = when (state) {
    SuitabilityState.EXCELLENT -> ScoreExcellent
    SuitabilityState.GOOD -> ScoreGood
    SuitabilityState.MODERATE -> ScoreModerate
    SuitabilityState.POOR -> ScorePoor
    SuitabilityState.NOT_RECOMMENDED -> ScoreNotRec
}
