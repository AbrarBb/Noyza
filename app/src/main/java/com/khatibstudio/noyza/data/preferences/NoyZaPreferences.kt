package com.khatibstudio.noyza.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.khatibstudio.noyza.domain.model.ActivityType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noyza_preferences")

@Singleton
class NoyZaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Onboarding
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")

        // User preferences
        val DEFAULT_ACTIVITY = stringPreferencesKey("default_activity")
        val USER_NAME = stringPreferencesKey("user_name")

        // Measurement
        val CALIBRATION_OFFSET = floatPreferencesKey("calibration_offset")

        // Notifications
        val NOTIF_HIGH_NOISE = booleanPreferencesKey("notif_high_noise")
        val NOTIF_SESSION_COMPLETE = booleanPreferencesKey("notif_session_complete")
        val NOTIF_DAILY_SUMMARY = booleanPreferencesKey("notif_daily_summary")
        val HIGH_NOISE_THRESHOLD_DB = floatPreferencesKey("high_noise_threshold_db")

        // Premium / Entitlements
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val IS_ADS_REMOVED = booleanPreferencesKey("is_ads_removed")
        val PREMIUM_SKU = stringPreferencesKey("premium_sku")

        // Ad tracking
        val LAST_INTERSTITIAL_SHOWN = longPreferencesKey("last_interstitial_shown")
        val ACTION_COUNT_SINCE_AD = intPreferencesKey("action_count_since_ad")

        // Theme
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    }

    private val dataStore = context.dataStore

    // ─── Onboarding ──────────────────────────────────────────────────────────

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    // ─── Default Activity ─────────────────────────────────────────────────────

    val defaultActivity: Flow<ActivityType> = dataStore.data
        .catchIOException()
        .map { prefs ->
            val name = prefs[DEFAULT_ACTIVITY] ?: ActivityType.STUDY.name
            ActivityType.fromName(name)
        }

    suspend fun setDefaultActivity(activity: ActivityType) {
        dataStore.edit { it[DEFAULT_ACTIVITY] = activity.name }
    }

    // ─── User Name ────────────────────────────────────────────────────────────

    val userName: Flow<String> = dataStore.data
        .catchIOException()
        .map { it[USER_NAME] ?: "" }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME] = name }
    }

    // ─── Calibration ──────────────────────────────────────────────────────────

    val calibrationOffset: Flow<Float> = dataStore.data
        .catchIOException()
        .map { it[CALIBRATION_OFFSET] ?: 0f }

    suspend fun setCalibrationOffset(offset: Float) {
        dataStore.edit { it[CALIBRATION_OFFSET] = offset }
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    val notifHighNoise: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { it[NOTIF_HIGH_NOISE] ?: true }

    val notifSessionComplete: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { it[NOTIF_SESSION_COMPLETE] ?: true }

    val notifDailySummary: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { it[NOTIF_DAILY_SUMMARY] ?: false }

    val highNoiseThresholdDb: Flow<Float> = dataStore.data
        .catchIOException()
        .map { it[HIGH_NOISE_THRESHOLD_DB] ?: 75f }

    suspend fun setNotifHighNoise(enabled: Boolean) {
        dataStore.edit { it[NOTIF_HIGH_NOISE] = enabled }
    }

    suspend fun setNotifSessionComplete(enabled: Boolean) {
        dataStore.edit { it[NOTIF_SESSION_COMPLETE] = enabled }
    }

    suspend fun setNotifDailySummary(enabled: Boolean) {
        dataStore.edit { it[NOTIF_DAILY_SUMMARY] = enabled }
    }

    // ─── Premium ──────────────────────────────────────────────────────────────

    val isPremium: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { it[IS_PREMIUM] ?: false }

    val isAdsRemoved: Flow<Boolean> = dataStore.data
        .catchIOException()
        .map { (it[IS_PREMIUM] ?: false) || (it[IS_ADS_REMOVED] ?: false) }

    suspend fun setPremium(isPremium: Boolean, sku: String = "") {
        dataStore.edit {
            it[IS_PREMIUM] = isPremium
            if (sku.isNotEmpty()) it[PREMIUM_SKU] = sku
        }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        dataStore.edit { it[IS_ADS_REMOVED] = removed }
    }

    // ─── Ad frequency tracking ────────────────────────────────────────────────

    val lastInterstitialShown: Flow<Long> = dataStore.data
        .catchIOException()
        .map { it[LAST_INTERSTITIAL_SHOWN] ?: 0L }

    val actionCountSinceAd: Flow<Int> = dataStore.data
        .catchIOException()
        .map { it[ACTION_COUNT_SINCE_AD] ?: 0 }

    suspend fun recordInterstitialShown() {
        dataStore.edit {
            it[LAST_INTERSTITIAL_SHOWN] = System.currentTimeMillis()
            it[ACTION_COUNT_SINCE_AD] = 0
        }
    }

    suspend fun incrementActionCount() {
        dataStore.edit {
            val current = it[ACTION_COUNT_SINCE_AD] ?: 0
            it[ACTION_COUNT_SINCE_AD] = current + 1
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    suspend fun clearAllData() {
        dataStore.edit { it.clear() }
    }

    private fun Flow<Preferences>.catchIOException() =
        catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
}
