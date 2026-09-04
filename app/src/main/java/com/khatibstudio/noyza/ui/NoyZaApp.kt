package com.khatibstudio.noyza.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.khatibstudio.noyza.ui.navigation.*
import com.khatibstudio.noyza.ui.screens.analytics.AnalyticsScreen
import com.khatibstudio.noyza.ui.screens.explore.ExploreScreen
import com.khatibstudio.noyza.ui.screens.explore.PlaceComparisonScreen
import com.khatibstudio.noyza.ui.screens.explore.PlaceDetailScreen
import com.khatibstudio.noyza.ui.screens.history.HistoryScreen
import com.khatibstudio.noyza.ui.screens.home.HomeScreen
import com.khatibstudio.noyza.ui.screens.onboarding.*
import com.khatibstudio.noyza.ui.screens.premium.PremiumScreen
import com.khatibstudio.noyza.ui.screens.profile.CalibrationScreen
import com.khatibstudio.noyza.ui.screens.profile.ProfileScreen
import com.khatibstudio.noyza.ui.screens.session.ActiveSessionScreen
import com.khatibstudio.noyza.ui.screens.session.SessionSummaryScreen
import com.khatibstudio.noyza.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoyZaApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()

    val isOnboardingComplete by appViewModel.isOnboardingComplete.collectAsState()
    val startDestination = if (isOnboardingComplete) Screen.Home.route
    else Screen.OnboardingWelcome.route

    // Track current route to control bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

    val bottomNavItems = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = "Home",
            iconOutlined = Icons.Outlined.Home,
            iconFilled = Icons.Filled.Home
        ),
        BottomNavItem(
            screen = Screen.Explore,
            label = "Explore",
            iconOutlined = Icons.Outlined.Explore,
            iconFilled = Icons.Filled.Explore
        ),
        BottomNavItem(
            screen = Screen.History,
            label = "History",
            iconOutlined = Icons.Outlined.History,
            iconFilled = Icons.Filled.History
        ),
        BottomNavItem(
            screen = Screen.Profile,
            label = "Profile",
            iconOutlined = Icons.Outlined.Person,
            iconFilled = Icons.Filled.Person
        ),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Lesson from Cyvia: hide bottom nav on sub-screens
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    tonalElevation = 8.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(
                // Only apply bottom padding when bottom nav is visible
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
                top = innerPadding.calculateTopPadding()
            ),
            enterTransition = { fadeIn(tween(250)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            // ─── Onboarding ──────────────────────────────────────────────────
            composable(Screen.OnboardingWelcome.route) {
                OnboardingWelcomeScreen(navController = navController)
            }
            composable(Screen.OnboardingPermission.route) {
                OnboardingPermissionScreen(navController = navController)
            }
            composable(Screen.OnboardingCalibration.route) {
                OnboardingCalibrationScreen(navController = navController)
            }
            composable(Screen.OnboardingActivity.route) {
                OnboardingActivityScreen(navController = navController)
            }
            composable(Screen.OnboardingNotifications.route) {
                OnboardingNotificationsScreen(
                    navController = navController,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                        }
                    }
                )
            }

            // ─── Main tabs ───────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Explore.route) {
                ExploreScreen(navController = navController)
            }
            composable(Screen.History.route) {
                HistoryScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }

            // ─── Sub-screens ─────────────────────────────────────────────────
            composable(Screen.ActiveSession.route) {
                ActiveSessionScreen(navController = navController)
            }
            composable(Screen.SessionSummary.route) { backStack ->
                val sessionId = backStack.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
                SessionSummaryScreen(
                    sessionId = sessionId,
                    navController = navController
                )
            }
            composable(Screen.PlaceDetail.route) { backStack ->
                val placeId = backStack.arguments?.getString("placeId")?.toLongOrNull() ?: 0L
                PlaceDetailScreen(
                    placeId = placeId,
                    navController = navController
                )
            }
            composable(Screen.PlaceComparison.route) {
                PlaceComparisonScreen(navController = navController)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(navController = navController)
            }
            composable(Screen.Premium.route) {
                PremiumScreen(navController = navController)
            }
            composable(Screen.Calibration.route) {
                CalibrationScreen(navController = navController)
            }
        }
    }
}
