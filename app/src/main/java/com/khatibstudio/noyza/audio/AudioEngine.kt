package com.khatibstudio.noyza.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Audio capture and dB estimation engine.
 *
 * Pipeline:
 *   Microphone → AudioRecord → PCM samples → RMS amplitude → Estimated dB
 *   → Exponential smoothing → Noise classification → UI emission
 *
 * PRIVACY: No audio data is ever stored or transmitted.
 * Only the calculated dB values are retained for session statistics.
 *
 * DISCLAIMER: Smartphone microphones are not calibrated instruments.
 * All readings are ESTIMATES. Accuracy varies by device.
 */
@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioEngine"

        // Audio configuration
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // Smoothing factor (0.0 = max smoothing, 1.0 = no smoothing)
        // 0.15 gives a smooth, responsive reading
        private const val SMOOTHING_ALPHA = 0.15f

        // Reference amplitude for dB calculation (16-bit PCM)
        private const val REFERENCE_AMPLITUDE = 32767.0

        // Minimum dB floor (to avoid -Infinity when silent)
        private const val MIN_DB = 20f
        private const val MAX_DB = 120f

        // Calibration offset range
        const val MIN_CALIBRATION_OFFSET = -10f
        const val MAX_CALIBRATION_OFFSET = 10f
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val fftProcessor = FftAudioProcessor(sampleRate = SAMPLE_RATE, fftSize = 1024)

    private val _rawDbFlow = MutableSharedFlow<Float>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _smoothedDbFlow = MutableSharedFlow<Float>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _soundProfileFlow = MutableSharedFlow<SoundProfile>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        _rawDbFlow.tryEmit(MIN_DB)
        _smoothedDbFlow.tryEmit(MIN_DB)
        _soundProfileFlow.tryEmit(SoundProfile())
    }

    /** Smoothed, calibrated dB reading. Use this for UI display. */
    val smoothedDb: Flow<Float> = _smoothedDbFlow.asSharedFlow()

    /** Raw (unsmoothed) dB reading. Useful for peak detection. */
    val rawDb: Flow<Float> = _rawDbFlow.asSharedFlow()

    /** Real-time frequency band analysis & sound character classification. */
    val soundProfile: Flow<SoundProfile> = _soundProfileFlow.asSharedFlow()

    private var smoothedValue = MIN_DB
    private var calibrationOffset = 0f

    /**
     * Set calibration offset (-10 to +10 dB).
     * Applied after smoothing.
     */
    fun setCalibrationOffset(offset: Float) {
        calibrationOffset = offset.coerceIn(MIN_CALIBRATION_OFFSET, MAX_CALIBRATION_OFFSET)
    }

    /**
     * Start capturing audio from the microphone.
     * Must be called from a coroutine. Runs on IO dispatcher.
     * Requires RECORD_AUDIO permission.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun startCapture() = withContext(Dispatchers.IO) {
        if (isRecording) return@withContext

        val bufferSize = max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            4096
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return@withContext
            }

            audioRecord?.startRecording()
            isRecording = true

            val buffer = ShortArray(bufferSize / 2)

            while (isRecording) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (readCount > 0) {
                    val rms = calculateRms(buffer, readCount)
                    val rawDb = amplitudeToDb(rms)
                    val clampedRaw = rawDb.coerceIn(MIN_DB, MAX_DB)

                    // Apply exponential moving average smoothing
                    smoothedValue = (SMOOTHING_ALPHA * clampedRaw) + ((1f - SMOOTHING_ALPHA) * smoothedValue)

                    // Apply calibration offset
                    val calibratedSmoothed = (smoothedValue + calibrationOffset).coerceIn(MIN_DB, MAX_DB)
                    val calibratedRaw = (clampedRaw + calibrationOffset).coerceIn(MIN_DB, MAX_DB)

                    _rawDbFlow.tryEmit(calibratedRaw)
                    _smoothedDbFlow.tryEmit(calibratedSmoothed)

                    // Run FFT spectral analysis
                    val profile = fftProcessor.process(buffer, readCount)
                    _soundProfileFlow.tryEmit(profile)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing RECORD_AUDIO permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture error", e)
        } finally {
            releaseAudioRecord()
        }
    }

    /**
     * Stop audio capture and release microphone immediately.
     * Call this when session ends or app goes to background.
     */
    fun stopCapture() {
        isRecording = false
        releaseAudioRecord()
        fftProcessor.reset()
        // Reset to floor value
        _smoothedDbFlow.tryEmit(MIN_DB)
        _rawDbFlow.tryEmit(MIN_DB)
        _soundProfileFlow.tryEmit(SoundProfile())
        smoothedValue = MIN_DB
    }

    private fun releaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }

    /**
     * Calculate RMS (Root Mean Square) amplitude from PCM samples.
     */
    private fun calculateRms(buffer: ShortArray, readCount: Int): Double {
        var sumOfSquares = 0.0
        for (i in 0 until readCount) {
            val sample = buffer[i].toDouble()
            sumOfSquares += sample * sample
        }
        return sqrt(sumOfSquares / readCount)
    }

    /**
     * Convert RMS amplitude to decibel (dB SPL estimate).
     * Result is an ESTIMATE — not a calibrated measurement.
     */
    private fun amplitudeToDb(rms: Double): Float {
        if (rms == 0.0) return MIN_DB
        val db = 20.0 * log10(rms / REFERENCE_AMPLITUDE)
        // Normalize to human-readable range (roughly 20–120 dB)
        // PCM max gives ~0 dBFS; we map to approximate dB SPL
        // Adding 94 aligns full-scale to ~94 dB SPL (0 dBFS = 94 dB SPL reference)
        return (db + 94.0).toFloat()
    }

    /**
     * Check if AudioRecord is currently active.
     */
    fun isActive(): Boolean = isRecording
}
