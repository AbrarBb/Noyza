package com.khatibstudio.noyza.ui.screens.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.PlaceCategory
import com.khatibstudio.noyza.ui.components.*
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.viewmodel.ExploreViewModel

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Find Your Best Place",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Choose an activity to see your best environments",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Activity filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ActivityType.entries.take(8)) { activity ->
                FilterChip(
                    selected = uiState.selectedActivity == activity,
                    onClick = { viewModel.selectActivity(activity) },
                    label = { Text("${activity.emoji} ${activity.displayName}") }
                )
            }
        }

        // Sort/filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sorts = listOf("Best Overall", "Quietest", "Most Stable", "Recent")
            itemsIndexed(sorts) { index, sort ->
                FilterChip(
                    selected = uiState.sortIndex == index,
                    onClick = { viewModel.selectSort(index) },
                    label = { Text(sort) }
                )
            }
        }

        if (uiState.places.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("📍", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your favorite places will appear here.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Save places after your sessions to compare and track them.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Home.route) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Measure a Place")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        "Recommended",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                val rankEmojis = listOf("🥇", "🥈", "🥉")
                itemsIndexed(uiState.places) { index, place ->
                    // Insert sponsored ad slot between #2 and #3
                    if (index == 2 && !uiState.isAdsRemoved && uiState.places.size >= 3) {
                        SponsoredAdSlot(modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }

                    PlaceRowCard(
                        place = place,
                        rankEmoji = rankEmojis.getOrNull(index),
                        onClick = {
                            navController.navigate(Screen.PlaceDetail.createRoute(place.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SponsoredAdSlot(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider()
        Text(
            "Sponsored",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(vertical = 4.dp)
        )
        AdBannerSlot(modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
    }
}
