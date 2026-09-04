package com.khatibstudio.noyza.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.khatibstudio.noyza.BuildConfig
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized AdMob ad manager.
 *
 * Rules enforced:
 * - Never show ads during active measurement
 * - Never show ads while Session Summary is open
 * - Interstitial fires strictly on return to Home after completing a session
 * - Interstitial cap: max 1 per session, never on first 2 sessions ever, never within 3–4 minutes
 * - All ads disabled immediately when Premium / Ads Removed is active
 * - Debug builds always use official Google test IDs
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NoyZaPreferences,
    private val sessionRepository: SessionRepository
) {
    companion object {
        private const val TAG = "AdManager"

        // Frequency cap: minimum 3.5 minutes (210 seconds) between interstitials
        private const val MIN_INTERSTITIAL_INTERVAL_MS = 210_000L

        // Never show interstitial on a user's first 2 sessions ever
        private const val MIN_SESSIONS_BEFORE_INTERSTITIAL = 3

        // Exponential backoff delays for ad preloading
        private val BACKOFF_DELAYS = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var interstitialAd: InterstitialAd? = null

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private var isActiveMeasurement = false

    /**
     * Initialize AdMob SDK and preload interstitial.
     * Call this from Application.onCreate()
     */
    fun initialize() {
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob initialized: ${initStatus.adapterStatusMap}")
            preloadInterstitial()
        }
    }

    /**
     * Signal that an active measurement session is running.
     * Guarantees zero ads can interrupt active measurement.
     */
    fun setMeasurementActive(active: Boolean) {
        isActiveMeasurement = active
    }

    /**
     * Show an interstitial ad upon session completion (when returning from Session Summary to Home).
     *
     * Will NOT show if:
     * - User has Premium / Ads Removed
     * - Active measurement is running
     * - User has completed <= 2 sessions ever (first 2 sessions rule)
     * - Shown less than 3.5 minutes ago (frequency cap rule)
     * - Ad is not yet loaded
     *
     * Calls [onDismissed] in all cases (shown, failed, or skipped) so navigation proceeds cleanly.
     */
    suspend fun tryShowSessionCompletionInterstitial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ): Boolean {
        if (isActiveMeasurement) {
            Log.d(TAG, "Skipping interstitial: measurement active")
            onDismissed()
            return false
        }
        if (preferences.isAdsRemoved.first()) {
            Log.d(TAG, "Skipping interstitial: ads removed")
            onDismissed()
            return false
        }

        // Rule: Never on a user's first 2 sessions ever
        val totalSessions = try {
            sessionRepository.getSessionCount()
        } catch (e: Exception) {
            0
        }
        if (totalSessions < MIN_SESSIONS_BEFORE_INTERSTITIAL) {
            Log.d(TAG, "Skipping interstitial: user has only $totalSessions sessions (min $MIN_SESSIONS_BEFORE_INTERSTITIAL required)")
            onDismissed()
            return false
        }

        // Rule: Never twice within 3–4 minutes
        val lastShown = preferences.lastInterstitialShown.first()
        val now = System.currentTimeMillis()
        if ((now - lastShown) < MIN_INTERSTITIAL_INTERVAL_MS) {
            Log.d(TAG, "Skipping interstitial: frequency cap active (${(now - lastShown) / 1000}s elapsed)")
            onDismissed()
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial ad not ready, preloading next")
            preloadInterstitial()
            onDismissed()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isInterstitialReady.value = false
                preloadInterstitial()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                _isInterstitialReady.value = false
                preloadInterstitial()
                onDismissed()
            }
        }

        ad.show(activity)
        preferences.recordInterstitialShown()
        return true
    }

    /**
     * Preload interstitial with exponential backoff on failure.
     */
    private fun preloadInterstitial(retryIndex: Int = 0) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded")
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed: ${error.message}")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    // Exponential backoff retry
                    val delay = BACKOFF_DELAYS.getOrElse(retryIndex) { BACKOFF_DELAYS.last() }
                    scope.launch {
                        delay(delay)
                        preloadInterstitial(minOf(retryIndex + 1, BACKOFF_DELAYS.size - 1))
                    }
                }
            }
        )
    }

    fun recordUserAction() {
        scope.launch { preferences.incrementActionCount() }
    }

    fun destroy() {
        scope.cancel()
    }
}
