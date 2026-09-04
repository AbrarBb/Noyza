package com.khatibstudio.noyza.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.khatibstudio.noyza.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingNotificationsScreen(
    navController: NavController,
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var highNoiseEnabled by remember { mutableStateOf(true) }
    var sessionReminderEnabled by remember { mutableStateOf(true) }
    var dailySummaryEnabled by remember { mutableStateOf(false) }

    // Runtime POST_NOTIFICATIONS permission (Android 13+)
    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Get useful noise alerts",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Stay informed about your environment without interrupting your focus.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Notification toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                NotificationToggleRow(
                    title = "High noise alerts",
                    description = "When your environment gets noisy during a session",
                    icon = Icons.Outlined.VolumeUp,
                    checked = highNoiseEnabled,
                    onCheckedChange = { highNoiseEnabled = it }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                NotificationToggleRow(
                    title = "Session reminders",
                    description = "When you've been measuring for a while",
                    icon = Icons.Outlined.Timer,
                    checked = sessionReminderEnabled,
                    onCheckedChange = { sessionReminderEnabled = it }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                NotificationToggleRow(
                    title = "Daily summary",
                    description = "Your environment score at end of day",
                    icon = Icons.Outlined.BarChart,
                    checked = dailySummaryEnabled,
                    onCheckedChange = { dailySummaryEnabled = it }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.completeOnboarding(
                    highNoiseEnabled = highNoiseEnabled,
                    dailySummaryEnabled = dailySummaryEnabled
                )
                // Request notification permission on Android 13+
                if (notifPermission != null && highNoiseEnabled) {
                    notifPermission.launchPermissionRequest()
                }
                onFinish()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Finish Setup", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            viewModel.completeOnboarding(
                highNoiseEnabled = false,
                dailySummaryEnabled = false
            )
            onFinish()
        }) {
            Text(
                "Maybe Later",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        OnboardingPageIndicator(currentPage = 3, totalPages = 4)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
