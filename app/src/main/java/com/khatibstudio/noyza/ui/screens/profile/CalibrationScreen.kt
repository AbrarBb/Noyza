package com.khatibstudio.noyza.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.audio.AudioEngine
import com.khatibstudio.noyza.ui.viewmodel.CalibrationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navController: NavController,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val currentOffset by viewModel.calibrationOffset.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("About Calibration") },
            text = {
                Text(
                    "Smartphone microphones vary significantly between devices and manufacturers. " +
                            "Calibration allows you to adjust Noyza's estimated readings to better match " +
                            "a known reference (e.g., another sound level app or meter).\n\n" +
                            "This does NOT turn your smartphone into a professionally calibrated sound level meter. " +
                            "All readings remain estimates for environment suitability purposes only."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Got it") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Microphone Calibration") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.ArrowBack, "Back")
                }
            },
            actions = {
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(Icons.Outlined.Info, "Information")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "Smartphone microphones vary between devices. Calibration allows you to adjust the displayed estimate when compared with a known reference.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // Current offset display
            Text(
                text = if (currentOffset >= 0) "+${currentOffset.toInt()} dB" else "${currentOffset.toInt()} dB",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                "Current calibration offset",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(40.dp))

            // Offset slider
            Slider(
                value = currentOffset,
                onValueChange = { viewModel.setOffset(it) },
                valueRange = AudioEngine.MIN_CALIBRATION_OFFSET..AudioEngine.MAX_CALIBRATION_OFFSET,
                steps = 3, // -10, -5, 0, +5, +10
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-10 dB", style = MaterialTheme.typography.labelSmall)
                Text("0 dB", style = MaterialTheme.typography.labelSmall)
                Text("+10 dB", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(32.dp))

            // Quick preset buttons
            Text(
                "Quick presets",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-10f, -5f, 0f, 5f, 10f).forEach { preset ->
                    OutlinedButton(
                        onClick = { viewModel.setOffset(preset) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (preset >= 0) "+${preset.toInt()}" else "${preset.toInt()}")
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Calibration")
            }
        }
    }
}
