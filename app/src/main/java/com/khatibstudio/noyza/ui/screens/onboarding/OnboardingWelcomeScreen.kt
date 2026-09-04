package com.khatibstudio.noyza.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.khatibstudio.noyza.ui.navigation.Screen
import com.khatibstudio.noyza.ui.theme.Teal40
import com.khatibstudio.noyza.ui.theme.Teal80

@Composable
fun OnboardingWelcomeScreen(navController: NavController) {
    // Subtle logo pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(
                tween(600), initialOffsetY = { it / 6 }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo mark
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(logoScale)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(Teal40, Teal80.copy(alpha = 0.6f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Sound wave + N representation
                    NoyZaLogoMark(size = 60.dp)
                }

                Spacer(Modifier.height(32.dp))

                // App name
                Text(
                    text = "Noyza",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Tagline
                Text(
                    text = "Find the right place\nfor what you need to do.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Spacer(Modifier.height(24.dp))

                // Description
                Text(
                    text = "Noyza measures the sound around you and helps you understand whether your environment is suitable for studying, working, relaxing, recording, and more.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(64.dp))

                // CTA button
                Button(
                    onClick = { navController.navigate(Screen.OnboardingPermission.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Page indicator
                OnboardingPageIndicator(currentPage = 0, totalPages = 4)
            }
        }
    }
}

@Composable
fun OnboardingPageIndicator(currentPage: Int, totalPages: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val width by animateDpAsState(
                targetValue = if (index == currentPage) 24.dp else 8.dp,
                animationSpec = tween(300),
                label = "indicator_width"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .background(
                        color = if (index == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}
