package com.khatibstudio.noyza.ui.screens.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.model.PlaceCategory
import com.khatibstudio.noyza.util.LocationHelper
import kotlinx.coroutines.launch

/**
 * Bottom sheet for saving a place after a session or quick measure.
 * Supports optional GPS location tagging for nearby place discovery and future maps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePlaceSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, category: PlaceCategory, notes: String, latitude: Double?, longitude: Double?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    var placeName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.OTHER) }

    var isTaggingLocation by remember { mutableStateOf(false) }
    var detectedCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var locationStatusMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Save this place",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(16.dp))

            // Place name
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it },
                label = { Text("Place Name") },
                placeholder = { Text("e.g. University Library") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // Category selector
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlaceCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        leadingIcon = {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // GPS Location Tagging Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "GPS Coordinates",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (detectedCoordinates != null) {
                            val (lat, lng) = detectedCoordinates!!
                            Text(
                                "Tagged: %.4f°, %.4f°".format(lat, lng),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else if (locationStatusMessage != null) {
                            Text(
                                locationStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        } else {
                            Text(
                                "Tag location for nearby discovery",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    if (isTaggingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else if (detectedCoordinates != null) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Tagged",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isTaggingLocation = true
                                    val coords = locationHelper.getCurrentCoordinates()
                                    isTaggingLocation = false
                                    if (coords != null) {
                                        detectedCoordinates = coords
                                    } else {
                                        locationStatusMessage = "Location unavailable or permission needed"
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tag GPS", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Best table near the window") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                maxLines = 3
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (placeName.isNotBlank()) {
                        onSave(
                            placeName.trim(),
                            selectedCategory,
                            notes.trim(),
                            detectedCoordinates?.first,
                            detectedCoordinates?.second
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = placeName.isNotBlank()
            ) {
                Text("Save Place", style = MaterialTheme.typography.titleSmall)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
