package com.khatibstudio.noyza.ui.screens.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.data.local.entity.CustomActivityEntity
import com.khatibstudio.noyza.domain.model.resolveActivityIcon

/**
 * Bottom sheet for defining and saving personalized noise activity profiles.
 * Allows power users (musicians, creators, sensory-sensitive individuals)
 * to define their own decibel tolerances and spike sensitivity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomActivitySheet(
    onDismiss: () -> Unit,
    onSave: (CustomActivityEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Music") }
    var idealMinDb by remember { mutableFloatStateOf(35f) }
    var idealMaxDb by remember { mutableFloatStateOf(55f) }
    var acceptableMaxDb by remember { mutableFloatStateOf(65f) }
    var spikeSensitivity by remember { mutableFloatStateOf(0.8f) }

    val iconOptions = listOf(
        "Music", "Mic", "Headphones", "Art", "Bed", "Meditation", "Laptop", "Book", "School", "Fitness"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Create Custom Activity",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Set tailored noise thresholds for your specific needs",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(Modifier.height(20.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name") },
                placeholder = { Text("e.g. Piano Practice, ASMR, Baby Sleep") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Icon Picker
            Text(
                "Select Icon",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(iconOptions) { iconName ->
                    FilterChip(
                        selected = selectedIcon == iconName,
                        onClick = { selectedIcon = iconName },
                        leadingIcon = {
                            Icon(
                                imageVector = resolveActivityIcon(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text(iconName) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Thresholds Sliders
            Text(
                "Ideal Noise Range: ${idealMinDb.toInt()} – ${idealMaxDb.toInt()} dB",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Noise within this range scores 85–100%",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(4.dp))
            RangeSlider(
                value = idealMinDb..idealMaxDb,
                onValueChange = { range ->
                    idealMinDb = range.start
                    idealMaxDb = range.endInclusive
                    if (acceptableMaxDb < idealMaxDb) {
                        acceptableMaxDb = idealMaxDb + 10f
                    }
                },
                valueRange = 20f..90f,
                steps = 13
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Maximum Acceptable Noise: ${acceptableMaxDb.toInt()} dB",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Levels above this start receiving heavy penalties",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = acceptableMaxDb,
                onValueChange = { acceptableMaxDb = it },
                valueRange = idealMaxDb..100f,
                steps = 10
            )

            Spacer(Modifier.height(12.dp))

            // Spike Sensitivity
            Text(
                "Spike Sensitivity: ${if (spikeSensitivity >= 0.9f) "High" else if (spikeSensitivity >= 0.7f) "Normal" else "Low"}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "How strictly sudden noise jumps penalize your score",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(4.dp))
            Slider(
                value = spikeSensitivity,
                onValueChange = { spikeSensitivity = it },
                valueRange = 0.5f..1.2f,
                steps = 6
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val entity = CustomActivityEntity(
                            name = name.trim(),
                            iconName = selectedIcon,
                            idealMinDb = idealMinDb,
                            idealMaxDb = idealMaxDb,
                            acceptableMaxDb = acceptableMaxDb,
                            spikeSensitivity = spikeSensitivity
                        )
                        onSave(entity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Save Custom Activity", style = MaterialTheme.typography.titleSmall)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
