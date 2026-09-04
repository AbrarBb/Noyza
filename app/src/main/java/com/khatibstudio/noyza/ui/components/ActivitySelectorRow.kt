package com.khatibstudio.noyza.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.model.ActivityProfile
import com.khatibstudio.noyza.domain.model.ActivityType

/**
 * Horizontally scrollable activity selector.
 * Displays built-in preset activities and user-created custom profiles,
 * with an Add (+) button for creating custom activities.
 */
@Composable
fun ActivitySelectorRow(
    selectedActivity: ActivityProfile,
    onActivitySelected: (ActivityProfile) -> Unit,
    modifier: Modifier = Modifier,
    customActivities: List<ActivityProfile> = emptyList(),
    onAddCustomClick: () -> Unit = {}
) {
    val allActivities = remember(customActivities) {
        ActivityType.entries + customActivities
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "What are you doing?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allActivities) { activity ->
                ActivityChip(
                    activity = activity,
                    isSelected = activity.displayName == selectedActivity.displayName,
                    onClick = { onActivitySelected(activity) }
                )
            }

            // Add Custom Activity Chip
            item {
                Card(
                    modifier = Modifier.clickable(onClick = onAddCustomClick),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add custom activity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Ideal environment label for selected activity
        Text(
            text = "Ideal: ~${selectedActivity.idealMinDb.toInt()}–${selectedActivity.idealMaxDb.toInt()} dB · Max ${selectedActivity.acceptableMaxDb.toInt()} dB",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ActivityChip(
    activity: ActivityProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )

    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val borderModifier = if (isSelected) Modifier.border(
        1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)
    ) else Modifier

    Card(
        modifier = Modifier
            .scale(scale)
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = activity.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
