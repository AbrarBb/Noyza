package com.khatibstudio.noyza.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.NoiseLevel
import com.khatibstudio.noyza.ui.components.*
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.theme.*
import com.khatibstudio.noyza.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showEndConfirm by remember { mutableStateOf(false) }

    // Start session when screen opens (get activity from navArgs or default)
    LaunchedEffect(Unit) {
        if (!uiState.isRunning) {
            viewModel.startSession(ActivityType.STUDY) // Will be passed from HomeScreen
        }
    }

    // Intercept back press
    BackHandler {
        if (uiState.isRunning) showEndConfirm = true
        else navController.popBackStack()
    }

    // High noise alert dialog
    if (uiState.showHighNoiseAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHighNoiseAlert() },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Noise Increased") },
            text = { Text(uiState.highNoiseAlertMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissHighNoiseAlert() }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissHighNoiseAlert()
                        scope.launch {
                            val sessionId = viewModel.endSession()
                            navController.navigate(Screen.SessionSummary.createRoute(sessionId)) {
                                popUpTo(Screen.ActiveSession.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("End Session")
                }
            }
        )
    }

    // End confirm dialog
    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End Session?") },
            text = { Text("Your session will be saved and summarized.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndConfirm = false
                        scope.launch {
                            val sessionId = viewModel.endSession()
                            navController.navigate(Screen.SessionSummary.createRoute(sessionId)) {
                                popUpTo(Screen.ActiveSession.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("End Session", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main layout — full screen, no bottom nav
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = uiState.activity.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${uiState.activity.displayName} Session",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                // Pause indicator
                if (uiState.isPaused) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer
                Text(
                    text = formatDuration(uiState.durationSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Live dB reading (large)
                val gaugeColor = noiseLevelColor(uiState.noiseLevel)
                val animatedDb by animateFloatAsState(
                    targetValue = uiState.currentDb,
                    animationSpec = tween(400),
                    label = "session_db"
                )

                Text(
                    text = "${animatedDb.toInt()} dB",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor
                    )
                )

                Spacer(Modifier.height(4.dp))

                // Noise level badge
                NoiseLevelStatusBadge(noiseLevel = uiState.noiseLevel)

                Spacer(Modifier.height(12.dp))

                // Suitability inline
                Text(
                    text = "Suitability: ${uiState.suitabilityResult.score}/100",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = scoreStateColor(uiState.suitabilityResult.state)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Live graph
                if (uiState.dbHistory.isNotEmpty()) {
                    SessionLiveGraph(
                        samples = uiState.dbHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Stats grid
                SessionStatsGrid(
                    averageDb = uiState.averageDb,
                    peakDb = uiState.peakDb,
                    minimumDb = uiState.minimumDb,
                    stabilityPercent = uiState.stabilityPercent
                )

                Spacer(Modifier.height(16.dp))

                // Frequency-aware sound character analysis
                FrequencyProfileBar(
                    soundProfile = uiState.soundProfile,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(16.dp))
            }

            // Bottom controls — Lesson from Cyvia: wrap in nav padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (uiState.isPaused) {
                    // Paused state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "Session Paused",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.resumeSession() },
                            modifier = Modifier.weight(2f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Resume Session")
                        }
                        OutlinedButton(
                            onClick = { showEndConfirm = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("End")
                        }
                    }
                } else {
                    // Active state
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.pauseSession() },
                            modifier = Modifier.weight(1.2f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pause", maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = { showEndConfirm = true },
                            modifier = Modifier.weight(1.8f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("End Session", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoiseLevelStatusBadge(noiseLevel: NoiseLevel) {
    val (color, container) = noiseLevelColorPair(noiseLevel)
    Surface(
        color = container,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = noiseLevel.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun SessionStatsGrid(
    averageDb: Float,
    peakDb: Float,
    minimumDb: Float,
    stabilityPercent: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SessionStat("Average", if (averageDb > 0) "${averageDb.toInt()} dB" else "--")
            VerticalDivider(modifier = Modifier.height(40.dp))
            SessionStat("Peak", if (peakDb > 0) "${peakDb.toInt()} dB" else "--")
            VerticalDivider(modifier = Modifier.height(40.dp))
            SessionStat("Lowest", if (minimumDb < Float.MAX_VALUE) "${minimumDb.toInt()} dB" else "--")
            VerticalDivider(modifier = Modifier.height(40.dp))
            SessionStat("Stability", if (stabilityPercent > 0) "${stabilityPercent.toInt()}%" else "--")
        }
    }
}

@Composable
private fun SessionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
