package com.khatibstudio.noyza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.engine.PlaceScheduleForecast

/**
 * Visual predictive scheduling card showing quietest visiting hours and peak noise forecasts.
 */
@Composable
fun BestTimeToVisitCard(
    forecast: PlaceScheduleForecast,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Best Time to Visit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Quietest window highlight badge
            Surface(
                color = Color(0xFF00E676).copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Quietest Hours",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        )
                        Text(
                            forecast.quietestWindow,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        "~${forecast.quietestAverageDb.toInt()} dB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Peak noise window
            Surface(
                color = Color(0xFFFF5252).copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                "Peak Distraction Hours",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                            )
                            Text(
                                forecast.peakWindow,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                    Text(
                        "~${forecast.peakAverageDb.toInt()} dB",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Diurnal hourly heat graph (08:00 to 22:00)
            Text(
                "Hourly Forecast (Daytime)",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val daytime = forecast.hourlySlots.filter { it.hourOfDay in 8..21 }
                daytime.forEach { slot ->
                    val color = when {
                        slot.isQuietest -> Color(0xFF00E676)
                        slot.isPeak -> Color(0xFFFF5252)
                        slot.expectedDb < 50f -> Color(0xFF00E5FF)
                        slot.expectedDb < 65f -> Color(0xFFFFB300)
                        else -> Color(0xFFFF5252)
                    }

                    val normalizedHeight = ((slot.expectedDb - 30f) / 50f).coerceIn(0.2f, 1f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(normalizedHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(color)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("8 AM", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline))
                Text("1 PM", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline))
                Text("6 PM", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline))
                Text("9 PM", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline))
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = forecast.recommendationSummary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
