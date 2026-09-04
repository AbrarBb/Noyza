package com.khatibstudio.noyza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.model.Place
import com.khatibstudio.noyza.domain.model.Session
import com.khatibstudio.noyza.domain.model.SuitabilityState
import com.khatibstudio.noyza.ui.components.formatDuration
import com.khatibstudio.noyza.ui.components.scoreStateColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionSummaryRowCard(
    session: Session,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity emoji
            Text(
                text = session.activityType.emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.activityType.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatDuration(session.durationSeconds),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${session.averageDb.toInt()} dB avg",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                if (session.placeName != null) {
                    Text(
                        text = session.placeName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Score badge
            val scoreState = SuitabilityState.fromScore(session.suitabilityScore)
            val scoreColor = scoreStateColor(scoreState)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${session.suitabilityScore}/100",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                )
                Text(
                    text = scoreState.label,
                    style = MaterialTheme.typography.labelSmall.copy(color = scoreColor)
                )
            }

            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PlaceRowCard(
    place: Place,
    onClick: () -> Unit,
    rankEmoji: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rankEmoji != null) {
                Text(
                    text = rankEmoji,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(12.dp))
            } else {
                Text(
                    text = place.category.emoji,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (place.averageDb > 0) {
                        Text(
                            text = "${place.averageDb.toInt()} dB avg",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    if (place.measurementCount > 0) {
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${place.measurementCount} measurements",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            if (place.bestSuitabilityScore > 0) {
                val scoreState = SuitabilityState.fromScore(place.bestSuitabilityScore)
                val scoreColor = scoreStateColor(scoreState)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${place.bestSuitabilityScore}/100",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    )
                    Text(
                        text = scoreState.label,
                        style = MaterialTheme.typography.labelSmall.copy(color = scoreColor)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
