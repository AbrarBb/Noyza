package com.khatibstudio.noyza.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.khatibstudio.noyza.BuildConfig
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
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
 * - Never show ads before session results
 * - Interstitial frequency cap: max 1 per [MIN_INTERSTITIAL_INTERVAL_MS] ms
 * - All ad units toggle off instantly when Premium is active
 * - Debug builds always use Google test IDs (Cyvia lesson)
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NoyZaPreferences
) {
    companion object {
        private const val TAG = "AdManager"

        // Minimum 3 minutes between interstitials + 3 user actions
        private const val MIN_INTERSTITIAL_INTERVAL_MS = 3 * 60 * 1000L
        private const val MIN_ACTIONS_BEFORE_INTERSTITIAL = 3

        // Exponential backoff delays for ad preloading
        private val BACKOFF_DELAYS = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L)
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private var isActiveMeasurement = false

    /**
     * Initialize AdMob SDK and preload ads.
     * Call this from Application.onCreate()
     */
    fun initialize() {
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob initialized: ${initStatus.adapterStatusMap}")
            preloadInterstitial()
            preloadRewarded()
        }
    }

    /**
     * Signal that a measurement session is active.
     * Prevents any interstitial from showing during measurement.
     */
    fun setMeasurementActive(active: Boolean) {
        isActiveMeasurement = active
    }

    /**
     * Show an interstitial ad if conditions are met.
     * Returns true if the ad was shown.
     *
     * Will NOT show if:
     * - User is Premium / Ads Removed
     * - Active measurement is running
     * - Frequency cap not met
     * - No ad loaded
     */
    suspend fun tryShowInterstitial(activity: Activity): Boolean {
        if (isActiveMeasurement) return false
        if (preferences.isAdsRemoved.first()) return false

        val lastShown = preferences.lastInterstitialShown.first()
        val actionCount = preferences.actionCountSinceAd.first()
        val now = System.currentTimeMillis()

        val timeOk = (now - lastShown) >= MIN_INTERSTITIAL_INTERVAL_MS
        val actionsOk = actionCount >= MIN_ACTIONS_BEFORE_INTERSTITIAL

        if (!timeOk || !actionsOk) return false

        val ad = interstitialAd ?: return false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isInterstitialReady.value = false
                preloadInterstitial() // Preload next one immediately
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                _isInterstitialReady.value = false
                preloadInterstitial()
            }
        }

        ad.show(activity)
        preferences.recordInterstitialShown()
        return true
    }

    /**
     * Show a rewarded ad. Call only when user explicitly opts in.
     */
    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        val ad = rewardedAd ?: run { onFailed(); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
                preloadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                _isRewardedReady.value = false
                preloadRewarded()
                onFailed()
            }
        }

        ad.show(activity) { _ -> onRewarded() }
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

    private fun preloadRewarded(retryIndex: Int = 0) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: ${error.message}")
                    rewardedAd = null
                    _isRewardedReady.value = false
                    val delay = BACKOFF_DELAYS.getOrElse(retryIndex) { BACKOFF_DELAYS.last() }
                    scope.launch {
                        delay(delay)
                        preloadRewarded(minOf(retryIndex + 1, BACKOFF_DELAYS.size - 1))
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
