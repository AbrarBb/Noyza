package com.khatibstudio.noyza.ui.screens.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khatibstudio.noyza.domain.model.PlaceCategory

/**
 * Bottom sheet for saving a place after a session or quick measure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePlaceSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, category: PlaceCategory, notes: String) -> Unit
) {
    var placeName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PlaceCategory.OTHER) }

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

            Spacer(Modifier.height(20.dp))

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
                        label = { Text("${category.emoji} ${category.displayName}") }
                    )
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
                maxLines = 4
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (placeName.isNotBlank()) {
                        onSave(placeName.trim(), selectedCategory, notes.trim())
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
