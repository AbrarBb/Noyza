package com.khatibstudio.noyza.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.domain.model.*
import com.khatibstudio.noyza.ui.components.*
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.viewmodel.HomeViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuickMeasureSheet by remember { mutableStateOf(false) }
    var quickMeasureResult by remember { mutableStateOf<Triple<SuitabilityResult, Float, Float>?>(null) }

    // Start/stop live monitoring based on screen visibility
    DisposableEffect(Unit) {
        viewModel.startLiveMonitoring()
        onDispose { viewModel.stopLiveMonitoring() }
    }

    // Quick Measure bottom sheet
    if (showQuickMeasureSheet) {
        QuickMeasureSheet(
            activity = uiState.selectedActivity,
            result = quickMeasureResult,
            isLoading = quickMeasureResult == null,
            onDismiss = {
                showQuickMeasureSheet = false
                quickMeasureResult = null
                viewModel.cancelQuickMeasure()
            },
            onSavePlace = { /* Navigate to save place flow */ },
            onMeasureAgain = {
                quickMeasureResult = null
                viewModel.startQuickMeasure { result, avg, peak ->
                    quickMeasureResult = Triple(result, avg, peak)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ─── Header ────────────────────────────────────────────────────────
        item {
            HomeHeader(
                greeting = uiState.greeting,
                isPremium = uiState.isPremium,
                onPremiumClick = { navController.navigate(Screen.Premium.route) },
                onSettingsClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        // ─── Activity Selector ────────────────────────────────────────────
        item {
            ActivitySelectorRow(
                selectedActivity = uiState.selectedActivity,
                onActivitySelected = { viewModel.selectActivity(it) }
            )
        }

        // ─── Live Noise Gauge ─────────────────────────────────────────────
        item {
            LiveNoiseGaugeCard(
                currentDb = uiState.currentDb,
                noiseLevel = uiState.noiseLevel,
                averageDb = uiState.averageDb,
                peakDb = uiState.peakDb,
                durationSeconds = uiState.durationSeconds,
                isActive = uiState.isActive
            )
        }

        // ─── Suitability Score ────────────────────────────────────────────
        item {
            SuitabilityScoreCard(
                result = uiState.suitabilityResult,
                activity = uiState.selectedActivity
            )
        }

        // ─── Recommendation Card ──────────────────────────────────────────
        item {
            RecommendationCard(result = uiState.suitabilityResult)
        }

        // ─── Ideal Range Info ─────────────────────────────────────────────
        item {
            IdealRangeCard(activity = uiState.selectedActivity)
        }

        // ─── Primary Actions ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screen.ActiveSession.route) },
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.hasMicPermission
                ) {
                    Text(
                        "Start Session",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                OutlinedButton(
                    onClick = {
                        showQuickMeasureSheet = true
                        viewModel.startQuickMeasure { result, avg, peak ->
                            quickMeasureResult = Triple(result, avg, peak)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.hasMicPermission
                ) {
                    Text(
                        "Quick",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        // ─── Recent Sessions ──────────────────────────────────────────────
        if (uiState.recentSessions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Sessions",
                    onSeeAll = { navController.navigate(Screen.History.route) }
                )
            }
            items(uiState.recentSessions) { session ->
                SessionSummaryRowCard(
                    session = session,
                    onClick = {
                        navController.navigate(Screen.SessionSummary.createRoute(session.id))
                    }
                )
            }
        }

        // ─── Saved Places ─────────────────────────────────────────────────
        if (uiState.savedPlaces.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your Best Places",
                    onSeeAll = { navController.navigate(Screen.Explore.route) }
                )
            }
            items(uiState.savedPlaces) { place ->
                PlaceRowCard(
                    place = place,
                    onClick = {
                        navController.navigate(Screen.PlaceDetail.createRoute(place.id))
                    }
                )
            }
        }

        // ─── Empty state ──────────────────────────────────────────────────
        if (uiState.recentSessions.isEmpty() && uiState.savedPlaces.isEmpty()) {
            item {
                HomeEmptyState()
            }
        }

        // ─── Ad banner (free users only, never over gauge) ────────────────
        if (!uiState.isAdsRemoved) {
            item {
                AdBannerSlot(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = "Find your best environment",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (!isPremium) {
            IconButton(onClick = onPremiumClick) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Upgrade to Premium",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IdealRangeCard(activity: ActivityType) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Ideal for ${activity.displayName}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "Approx. ${activity.idealMinDb.toInt()}–${activity.idealMaxDb.toInt()} dB",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No sessions yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Start a session to understand your environment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSeeAll) {
            Text("See All", style = MaterialTheme.typography.labelLarge)
        }
    }
}
