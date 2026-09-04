package com.khatibstudio.noyza.ui.screens.explore

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
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
                    leadingIcon = {
                        Icon(
                            imageVector = activity.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(activity.displayName) }
                )
            }
        }

        // Sort/filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sorts = listOf("Best Match", "Quietest", "Nearby")
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
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
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

                itemsIndexed(uiState.places) { index, place ->
                    PlaceRowCard(
                        place = place,
                        rankNumber = index + 1,
                        distanceLabel = viewModel.getDistanceLabel(place),
                        onClick = {
                            navController.navigate(Screen.PlaceDetail.createRoute(place.id))
                        }
                    )
                }

                if (!uiState.isAdsRemoved && uiState.places.isNotEmpty()) {
                    item {
                        AdBannerSlot(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
