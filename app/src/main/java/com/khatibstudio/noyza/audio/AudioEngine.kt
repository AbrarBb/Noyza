package com.khatibstudio.noyza.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val engineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val activeClients = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val captureLock = Any()
    private var captureJob: kotlinx.coroutines.Job? = null

    /**
     * Start capturing audio from the microphone.
     * Uses client tagging so navigation transitions or multiple concurrent callers
     * (e.g. HomeScreen -> ActiveSessionScreen) do not terminate recording.
     * Requires RECORD_AUDIO permission.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startCapture(clientTag: String = "default") {
        synchronized(captureLock) {
            activeClients.add(clientTag)
            if (captureJob == null || captureJob?.isActive != true) {
                captureJob = engineScope.launch {
                    runCaptureLoop()
                }
            }
        }
    }

    private suspend fun runCaptureLoop() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var record: AudioRecord? = null
        try {
            val bufferSize = max(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
                4096
            )

            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                record.release()
                return@withContext
            }

            synchronized(captureLock) {
                audioRecord = record
                isRecording = true
            }

            record.startRecording()

            val buffer = ShortArray(bufferSize / 2)

            while (coroutineContext.isActive && isRecording && activeClients.isNotEmpty()) {
                val readCount = record.read(buffer, 0, buffer.size)

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
                } else if (readCount < 0) {
                    Log.w(TAG, "AudioRecord read returned error code: $readCount")
                    kotlinx.coroutines.delay(50)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing RECORD_AUDIO permission", e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal coroutine cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture error", e)
        } finally {
            synchronized(captureLock) {
                isRecording = false
                try {
                    record?.stop()
                    record?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing AudioRecord", e)
                }
                if (audioRecord === record) {
                    audioRecord = null
                }
            }
            fftProcessor.reset()
            _smoothedDbFlow.tryEmit(MIN_DB)
            _rawDbFlow.tryEmit(MIN_DB)
            _soundProfileFlow.tryEmit(SoundProfile())
            smoothedValue = MIN_DB
        }
    }

    /**
     * Stop audio capture for a specific client.
     * If clientTag is null, stops all clients and releases microphone immediately.
     * Microphone is only released when NO active clients remain.
     */
    fun stopCapture(clientTag: String? = null) {
        synchronized(captureLock) {
            if (clientTag == null) {
                activeClients.clear()
            } else {
                activeClients.remove(clientTag)
            }
            if (activeClients.isEmpty()) {
                isRecording = false
                captureJob?.cancel()
                captureJob = null
            }
        }
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
