package com.khatibstudio.noyza.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized haptic and tactile feedback engine for accessibility and alert cues.
 *
 * Provides distinct physical feedback patterns for:
 * - Noise spike alerts (critical for deaf/hard-of-hearing or sensory-monitoring users)
 * - Suitability threshold drops
 * - Tactile UI clicks/ticks
 */
@Singleton
class NoyZaHapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NoyZaPreferences
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var hapticsEnabled = true

    init {
        CoroutineScope(Dispatchers.IO).launch {
            preferences.hapticAlertsEnabled.collect { enabled ->
                hapticsEnabled = enabled
            }
        }
    }

    /**
     * Distinct double-pulse pattern when a sudden noise spike occurs.
     * Pattern: 60ms pulse, 50ms pause, 100ms pulse.
     */
    fun vibrateNoiseSpike() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 60, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 60, 50, 100), -1)
        }
    }

    /**
     * Soft single warning pulse when suitability degrades.
     */
    fun vibrateThresholdWarning() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(80, 160)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    /**
     * Subtle tactile tick on button or slider action.
     */
    fun tick() {
        if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(15, 80)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(15)
        }
    }
}
