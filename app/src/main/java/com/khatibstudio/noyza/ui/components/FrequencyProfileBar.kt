package com.khatibstudio.noyza.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.audio.SoundCharacter
import com.khatibstudio.noyza.audio.SoundProfile

/**
 * Visual 3-band acoustic frequency spectrum and sound character widget.
 *
 * Shows real-time Low (HVAC), Mid (Speech), and High (Clatter) energy distributions
 * with TalkBack accessibility semantics and modern Material Design styling.
 */
@Composable
fun FrequencyProfileBar(
    soundProfile: SoundProfile,
    modifier: Modifier = Modifier
) {
    val lowWeight by animateFloatAsState(
        targetValue = maxOf(0.05f, soundProfile.lowPercent / 100f),
        animationSpec = tween(150),
        label = "lowWeight"
    )
    val midWeight by animateFloatAsState(
        targetValue = maxOf(0.05f, soundProfile.midPercent / 100f),
        animationSpec = tween(150),
        label = "midWeight"
    )
    val highWeight by animateFloatAsState(
        targetValue = maxOf(0.05f, soundProfile.highPercent / 100f),
        animationSpec = tween(150),
        label = "highWeight"
    )

    val lowColor = Color(0xFF00E5FF)       // Cyan / HVAC
    val midColor = Color(0xFF7C4DFF)       // Violet / Speech
    val highColor = Color(0xFFFFB300)      // Amber / Clatter

    val characterIcon = when (soundProfile.character) {
        SoundCharacter.SPEECH_HEAVY -> Icons.Outlined.RecordVoiceOver
        SoundCharacter.LOW_RUMBLE -> Icons.Outlined.GraphicEq
        SoundCharacter.SHARP_CLATTER -> Icons.Outlined.Speed
        SoundCharacter.BALANCED -> Icons.Outlined.Hearing
    }

    val accessibilityText = "Acoustic spectrum: ${soundProfile.character.displayName}. " +
            "Low frequency HVAC: ${soundProfile.lowPercent.toInt()}%, " +
            "Mid frequency speech: ${soundProfile.midPercent.toInt()}%, " +
            "High frequency clatter: ${soundProfile.highPercent.toInt()}%"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityText },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Character badge + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = characterIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Sound Character",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = soundProfile.character.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Multi-segment spectrum bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .weight(lowWeight)
                        .fillMaxHeight()
                        .background(lowColor)
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(midWeight)
                        .fillMaxHeight()
                        .background(midColor)
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(highWeight)
                        .fillMaxHeight()
                        .background(highColor)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Band breakdown percentages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BandIndicator(
                    color = lowColor,
                    label = "Low (HVAC)",
                    shortLabel = "Low",
                    percent = soundProfile.lowPercent.toInt()
                )
                BandIndicator(
                    color = midColor,
                    label = "Mid (Speech)",
                    shortLabel = "Mid",
                    percent = soundProfile.midPercent.toInt()
                )
                BandIndicator(
                    color = highColor,
                    label = "High (Clatter)",
                    shortLabel = "High",
                    percent = soundProfile.highPercent.toInt()
                )
            }
        }
    }
}

@Composable
private fun BandIndicator(
    color: Color,
    label: String,
    shortLabel: String,
    percent: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$shortLabel $percent%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
