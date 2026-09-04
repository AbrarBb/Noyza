package com.khatibstudio.noyza.ui.navigation

/**
 * All navigation routes in the Noyza app.
 */
sealed class Screen(val route: String) {
    // ─── Onboarding ──────────────────────────────────────────────────────────
    object OnboardingWelcome : Screen("onboarding_welcome")
    object OnboardingPermission : Screen("onboarding_permission")
    object OnboardingActivity : Screen("onboarding_activity")
    object OnboardingNotifications : Screen("onboarding_notifications")

    // ─── Main Bottom Nav Tabs ─────────────────────────────────────────────────
    object Home : Screen("home")
    object Explore : Screen("explore")
    object History : Screen("history")
    object Profile : Screen("profile")

    // ─── Sub-screens (hide bottom nav) ────────────────────────────────────────
    object ActiveSession : Screen("active_session")
    object SessionSummary : Screen("session_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_summary/$sessionId"
    }
    object PlaceDetail : Screen("place_detail/{placeId}") {
        fun createRoute(placeId: Long) = "place_detail/$placeId"
    }
    object PlaceComparison : Screen("place_comparison")
    object Analytics : Screen("analytics")
    object Premium : Screen("premium")
    object Calibration : Screen("calibration")
    object CustomActivity : Screen("custom_activity")
    object Settings : Screen("settings")
}

/**
 * Bottom navigation bar items (top-level destinations).
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector,
    val iconFilled: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Screens that should show the bottom navigation bar.
 */
val topLevelRoutes = setOf(
    Screen.Home.route,
    Screen.Explore.route,
    Screen.History.route,
    Screen.Profile.route
)
