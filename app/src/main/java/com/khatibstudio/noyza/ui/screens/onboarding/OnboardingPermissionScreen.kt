package com.khatibstudio.noyza.ui.screens.onboarding

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.khatibstudio.noyza.ui.navigation.Screen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingPermissionScreen(navController: NavController) {
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500), initialOffsetY = { it / 8 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mic icon in teal circle
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Microphone",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Measure your environment",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Noyza needs microphone access to estimate ambient sound levels.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Privacy notice card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔒 Privacy First",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Noyza never records or stores your audio. The microphone is used only to calculate estimated noise levels, which are processed entirely on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                when {
                    micPermission.status.isGranted -> {
                        // Permission already granted
                        Button(
                            onClick = { navController.navigate(Screen.OnboardingActivity.route) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Continue", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    micPermission.status.shouldShowRationale -> {
                        // User denied once — explain why
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Microphone access is required to measure environmental sound.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { micPermission.launchPermissionRequest() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Allow Microphone")
                            }
                        }
                    }
                    else -> {
                        // Not yet asked or permanently denied
                        Button(
                            onClick = {
                                if (micPermission.status.isGranted) {
                                    navController.navigate(Screen.OnboardingActivity.route)
                                } else {
                                    micPermission.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow Microphone", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Handle case where permission is permanently denied
                if (!micPermission.status.isGranted && !micPermission.status.shouldShowRationale) {
                    TextButton(
                        onClick = {
                            // Navigate to app settings
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open Settings")
                    }
                }

                Spacer(Modifier.height(16.dp))

                LaunchedEffect(micPermission.status) {
                    if (micPermission.status.isGranted) {
                        navController.navigate(Screen.OnboardingActivity.route)
                    }
                }

                OnboardingPageIndicator(currentPage = 1, totalPages = 4)
            }
        }
    }
}
