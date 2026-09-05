package com.khatibstudio.noyza.ui.screens.session

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.domain.model.SuitabilityState
import com.khatibstudio.noyza.ui.components.*
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.viewmodel.SessionSummaryViewModel
import com.khatibstudio.noyza.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    sessionId: Long,
    navController: NavController,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSavePlaceSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity

    val onDone: () -> Unit = {
        viewModel.onDoneClicked(activity) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    BackHandler {
        onDone()
    }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Save place sheet
    if (showSavePlaceSheet) {
        SavePlaceSheet(
            onDismiss = { showSavePlaceSheet = false },
            onSave = { name, category, notes, lat, lng ->
                viewModel.savePlace(sessionId, name, category, notes, lat, lng)
                showSavePlaceSheet = false
            }
        )
    }

    if (uiState.session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = uiState.session!!
    val scoreState = SuitabilityState.fromScore(session.suitabilityScore)
    val scoreColor = scoreStateColor(scoreState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = session.activityType.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${session.activityType.displayName} Complete")
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── Score ────────────────────────────────────────────────────
            Text(
                text = "${session.suitabilityScore}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    fontSize = 72.sp
                )
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (scoreState) {
                    SuitabilityState.EXCELLENT -> "Excellent environment for ${session.activityType.displayName.lowercase()}"
                    SuitabilityState.GOOD -> "Good environment for ${session.activityType.displayName.lowercase()}"
                    SuitabilityState.MODERATE -> "Acceptable environment"
                    SuitabilityState.POOR -> "Challenging environment"
                    SuitabilityState.NOT_RECOMMENDED -> "Difficult environment"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ─── Stats ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Session Statistics",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat("Duration", formatDuration(session.durationSeconds))
                        SummaryStat("Average", "${session.averageDb.toInt()} dB")
                        SummaryStat("Peak", "${session.maximumDb.toInt()} dB")
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryStat("Lowest", "${session.minimumDb.toInt()} dB")
                        SummaryStat("Stability", "${session.stabilityScore.toInt()}%")
                        SummaryStat("Samples", "${session.sampleCount}")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Noise Timeline Graph ─────────────────────────────────────
            if (uiState.samples.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Noise Timeline",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        SessionLiveGraph(
                            samples = uiState.samples,
                            isLive = false,
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ─── Environment Breakdown ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Environment Breakdown",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(12.dp))

                    BreakdownBar("Quiet", session.quietPercent, QuietGreenLight)
                    BreakdownBar("Moderate", session.moderatePercent, ModerateAmberLight)
                    BreakdownBar("Loud", session.loudPercent, LoudOrangeLight)
                    BreakdownBar("Very Loud", session.veryLoudPercent, VeryLoudRedLight)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ─── Recommendation note ──────────────────────────────────────
            val recoText = when {
                session.veryLoudPercent > 10 -> "There were significant noise spikes during this session."
                session.loudPercent > 30 -> "The environment was noisier than ideal for this activity."
                session.stabilityScore > 85 -> "Your environment was mostly stable with minimal disturbances."
                else -> "The environment was generally suitable with occasional noise variations."
            }
            Text(
                text = recoText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ─── Actions ──────────────────────────────────────────────────
            Button(
                onClick = { showSavePlaceSheet = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save This Place")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Done")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun BreakdownBar(label: String, percent: Float, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(70.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Text(
            text = "${percent.toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
