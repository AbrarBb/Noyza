package com.khatibstudio.noyza.ui.screens.profile

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.BuildConfig
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete All Data?") },
            text = { Text("This will permanently delete all your sessions, saved places, and history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Premium banner
        if (!uiState.isPremium) {
            PremiumBannerCard(onClick = { navController.navigate(Screen.Premium.route) })
        } else {
            PremiumActiveBadge()
        }

        Spacer(Modifier.height(8.dp))

        // ─── Personalization ──────────────────────────────────────────────
        ProfileSection(title = "Personalization") {
            ProfileSettingRow(
                icon = uiState.defaultActivity.icon,
                title = "Default Activity",
                subtitle = uiState.defaultActivity.displayName,
                onClick = { /* Open activity picker */ }
            )
        }

        // ─── Measurement ──────────────────────────────────────────────────
        ProfileSection(title = "Measurement") {
            ProfileSettingRow(
                icon = Icons.Outlined.Tune,
                title = "Microphone Calibration",
                subtitle = "${if (uiState.calibrationOffset >= 0) "+" else ""}${uiState.calibrationOffset.toInt()} dB",
                onClick = { navController.navigate(Screen.Calibration.route) }
            )
            ProfileSettingRow(
                icon = Icons.Outlined.Mic,
                title = "Microphone Info",
                subtitle = "Estimated readings — not a calibrated meter",
                onClick = { /* Show info dialog */ }
            )
        }

        // ─── Notifications ────────────────────────────────────────────────
        ProfileSection(title = "Notifications") {
            ProfileToggleRow(
                icon = Icons.Outlined.VolumeUp,
                title = "High Noise Alerts",
                checked = uiState.notifHighNoise,
                onCheckedChange = { viewModel.setNotifHighNoise(it) }
            )
            ProfileToggleRow(
                icon = Icons.Outlined.CalendarToday,
                title = "Daily Summary",
                checked = uiState.notifDailySummary,
                onCheckedChange = { viewModel.setNotifDailySummary(it) }
            )
        }

        // ─── Accessibility & Sensory ───────────────────────────────────────
        ProfileSection(title = "Accessibility & Sensory") {
            ProfileToggleRow(
                icon = Icons.Outlined.Vibration,
                title = "Haptic Spike Alerts",
                checked = uiState.hapticAlertsEnabled,
                onCheckedChange = { viewModel.setHapticAlertsEnabled(it) }
            )
            ProfileToggleRow(
                icon = Icons.Outlined.AccessibilityNew,
                title = "Sensory-Friendly Mode",
                checked = uiState.sensoryFriendlyMode,
                onCheckedChange = { viewModel.setSensoryFriendlyMode(it) }
            )
        }

        // ─── Data ─────────────────────────────────────────────────────────
        ProfileSection(title = "Data") {
            ProfileSettingRow(
                icon = Icons.Outlined.FileDownload,
                title = "Export CSV",
                subtitle = if (uiState.isPremium) "Export all sessions" else "Premium feature",
                onClick = { if (uiState.isPremium) viewModel.exportCsv(context) }
            )
            ProfileSettingRow(
                icon = Icons.Outlined.Delete,
                title = "Delete All Data",
                subtitle = "Permanently delete sessions and places",
                onClick = { showDeleteConfirm = true },
                isDestructive = true
            )
        }

        // ─── About ────────────────────────────────────────────────────────
        ProfileSection(title = "About") {
            ProfileSettingRow(
                icon = Icons.Outlined.PrivacyTip,
                title = "Privacy Policy",
                onClick = { /* Open URL */ }
            )
            ProfileSettingRow(
                icon = Icons.Outlined.Article,
                title = "Terms of Use",
                onClick = { /* Open URL */ }
            )
            ProfileSettingRow(
                icon = Icons.Outlined.Mail,
                title = "Contact Khatib Studio",
                onClick = { /* Open email */ }
            )
            ProfileSettingRow(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = BuildConfig.VERSION_NAME,  // Never hardcoded — tied to BuildConfig
                onClick = {}
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PremiumBannerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Noyza Premium",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Unlock advanced features and go ad-free",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PremiumActiveBadge() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Noyza Premium Active",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
