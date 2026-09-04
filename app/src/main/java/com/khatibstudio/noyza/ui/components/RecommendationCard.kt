package com.khatibstudio.noyza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.model.RecommendationType
import com.khatibstudio.noyza.domain.model.SuitabilityResult
import com.khatibstudio.noyza.ui.theme.*

/**
 * Recommendation card — communicates the primary recommendation to the user.
 * Uses text + emoji + color (never color alone).
 */
@Composable
fun RecommendationCard(
    result: SuitabilityResult,
    modifier: Modifier = Modifier
) {
    val (emoji, title, containerColor, contentColor) = when (result.recommendation) {
        RecommendationType.RECOMMENDED -> Quadruple(
            "✅",
            "Recommended",
            QuietGreenContainer,
            QuietGreen
        )
        RecommendationType.CONSIDER_QUIETER -> Quadruple(
            "⚠️",
            "Consider a quieter place",
            ModerateAmberContainer,
            ModerateAmber
        )
        RecommendationType.NOT_IDEAL -> Quadruple(
            "🔴",
            "Not Ideal",
            VeryLoudRedContainer,
            VeryLoudRed
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                )
                Text(
                    text = buildRecommendationBody(result),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor.copy(alpha = 0.85f)
                    )
                )
            }
        }
    }
}

private fun buildRecommendationBody(result: SuitabilityResult): String = when (result.recommendation) {
    RecommendationType.RECOMMENDED ->
        "This environment is well suited for ${result.activity.displayName.lowercase()}."
    RecommendationType.CONSIDER_QUIETER ->
        "Noise has remained relatively high. Consider finding a quieter spot."
    RecommendationType.NOT_IDEAL ->
        "This environment may make ${result.activity.displayName.lowercase()} significantly more difficult."
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
