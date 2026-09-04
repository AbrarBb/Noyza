package com.khatibstudio.noyza.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.audio.AudioEngine
import com.khatibstudio.noyza.data.local.entity.NoiseSampleEntity
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.engine.SuitabilityEngine
import com.khatibstudio.noyza.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class SessionUiState(
    val activity: ActivityType = ActivityType.STUDY,
    val currentDb: Float = 0f,
    val averageDb: Float = 0f,
    val peakDb: Float = 0f,
    val minimumDb: Float = Float.MAX_VALUE,
    val noiseLevel: NoiseLevel = NoiseLevel.QUIET,
    val suitabilityResult: SuitabilityResult = SuitabilityResult(),
    val stabilityPercent: Float = 100f,
    val durationSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val dbHistory: List<Float> = emptyList(),    // For graph
    val timeHistory: List<Long> = emptyList(),   // Timestamps for graph
    val sessionId: Long? = null,
    val showHighNoiseAlert: Boolean = false,
    val highNoiseAlertMessage: String = "",
    val soundProfile: com.khatibstudio.noyza.audio.SoundProfile = com.khatibstudio.noyza.audio.SoundProfile()
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    application: Application,
    private val audioEngine: AudioEngine,
    private val suitabilityEngine: SuitabilityEngine,
    private val sessionRepository: SessionRepository,
    private val preferences: NoyZaPreferences,
    private val hapticManager: com.khatibstudio.noyza.util.NoyZaHapticManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var captureJob: Job? = null
    private var timerJob: Job? = null
    private var sessionStartTime = 0L
    private var savedSeconds = 0L     // Accumulated paused time

    private val dbSamples = mutableListOf<Float>()
    private val graphSamples = mutableListOf<Float>()
    private val graphTimestamps = mutableListOf<Long>()
    private var savedSessionId: Long? = null

    // High noise alert tracking
    private var highNoiseStartTime = 0L
    private val HIGH_NOISE_ALERT_THRESHOLD_SECONDS = 180L // 3 minutes
    private var lastAlertedScore = 100

    fun startSession(activity: ActivityType) {
        if (!hasAudioPermission()) return

        _uiState.update { it.copy(
            activity = activity,
            isRunning = true,
            isPaused = false,
            currentDb = 0f,
            averageDb = 0f,
            peakDb = 0f,
            minimumDb = Float.MAX_VALUE,
            dbHistory = emptyList(),
            timeHistory = emptyList(),
            durationSeconds = 0L
        )}

        dbSamples.clear()
        graphSamples.clear()
        graphTimestamps.clear()
        sessionStartTime = System.currentTimeMillis()
        savedSeconds = 0L

        // Create session record in DB
        viewModelScope.launch {
            val sessionId = sessionRepository.saveSession(Session(
                activityType = activity,
                startTime = sessionStartTime,
                durationSeconds = 0
            ))
            savedSessionId = sessionId
            _uiState.update { it.copy(sessionId = sessionId) }
        }

        startCapture()
        startTimer()
    }

    private fun startCapture() {
        captureJob?.cancel()
        captureJob = viewModelScope.launch(Dispatchers.IO) {
            audioEngine.startCapture()
        }

        viewModelScope.launch {
            audioEngine.soundProfile.collect { profile ->
                _uiState.update { it.copy(soundProfile = profile) }
            }
        }

        viewModelScope.launch {
            audioEngine.smoothedDb.collect { db ->
                if (!_uiState.value.isRunning || _uiState.value.isPaused) return@collect

                dbSamples.add(db)
                graphSamples.add(db)
                graphTimestamps.add(System.currentTimeMillis())

                // Keep graph to 300 points max
                if (graphSamples.size > 300) {
                    graphSamples.removeAt(0)
                    graphTimestamps.removeAt(0)
                }

                val state = _uiState.value
                val newAvg = dbSamples.average().toFloat()
                val newPeak = max(state.peakDb, db)
                val newMin = if (state.minimumDb == Float.MAX_VALUE) db
                else min(state.minimumDb, db)

                val stability = suitabilityEngine.calculateStability(dbSamples.takeLast(50))
                val loudTime = dbSamples.count { it >= 68f } / dbSamples.size.toFloat() * 100f

                val suitability = suitabilityEngine.calculate(
                    activity = state.activity,
                    averageDb = newAvg,
                    peakDb = newPeak,
                    minimumDb = newMin,
                    stabilityPercent = stability,
                    loudTimePercent = loudTime,
                    samples = dbSamples.takeLast(100),
                    soundCharacter = state.soundProfile.character
                )

                _uiState.update { it.copy(
                    currentDb = db,
                    averageDb = newAvg,
                    peakDb = newPeak,
                    minimumDb = newMin,
                    noiseLevel = NoiseLevel.fromDb(db),
                    suitabilityResult = suitability,
                    stabilityPercent = stability,
                    dbHistory = graphSamples.toList(),
                    timeHistory = graphTimestamps.toList()
                )}

                checkHighNoiseAlert(db, suitability.score)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        val timerStart = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && !_uiState.value.isPaused) {
                delay(1000)
                val elapsed = savedSeconds + (System.currentTimeMillis() - timerStart) / 1000L
                _uiState.update { it.copy(durationSeconds = elapsed) }
            }
        }
    }

    fun pauseSession() {
        savedSeconds = _uiState.value.durationSeconds
        timerJob?.cancel()
        audioEngine.stopCapture()
        captureJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeSession() {
        _uiState.update { it.copy(isPaused = false) }
        startCapture()
        startTimer()
    }

    suspend fun endSession(): Long {
        timerJob?.cancel()
        audioEngine.stopCapture()
        captureJob?.cancel()

        val state = _uiState.value
        val endTime = System.currentTimeMillis()

        val stability = suitabilityEngine.calculateStability(dbSamples)
        val loudTime = dbSamples.count { it >= 68f } / dbSamples.size.toFloat().coerceAtLeast(1f) * 100f
        val finalSuitability = suitabilityEngine.calculate(
            activity = state.activity,
            averageDb = state.averageDb,
            peakDb = state.peakDb,
            minimumDb = if (state.minimumDb == Float.MAX_VALUE) 0f else state.minimumDb,
            stabilityPercent = stability,
            loudTimePercent = loudTime,
            samples = dbSamples
        )

        val session = Session(
            id = savedSessionId ?: 0L,
            activityType = state.activity,
            startTime = sessionStartTime,
            endTime = endTime,
            durationSeconds = state.durationSeconds,
            averageDb = state.averageDb,
            minimumDb = if (state.minimumDb == Float.MAX_VALUE) 0f else state.minimumDb,
            maximumDb = state.peakDb,
            stabilityScore = stability,
            suitabilityScore = finalSuitability.score,
            quietPercent = finalSuitability.quietPercent,
            moderatePercent = finalSuitability.moderatePercent,
            loudPercent = finalSuitability.loudPercent,
            veryLoudPercent = finalSuitability.veryLoudPercent,
            sampleCount = dbSamples.size
        )

        val sessionId = sessionRepository.saveSession(session)

        // Save noise samples
        if (dbSamples.isNotEmpty()) {
            sessionRepository.saveSamples(sessionId, dbSamples.takeLast(300))
        }

        _uiState.update { it.copy(isRunning = false, isPaused = false) }

        return sessionId
    }

    fun dismissHighNoiseAlert() {
        _uiState.update { it.copy(showHighNoiseAlert = false) }
        lastAlertedScore = _uiState.value.suitabilityResult.score
    }

    private fun checkHighNoiseAlert(db: Float, currentScore: Int) {
        val state = _uiState.value
        if (!state.isRunning || state.isPaused) return

        // Alert if score dropped significantly
        val scoreDrop = lastAlertedScore - currentScore
        if (scoreDrop >= 20 && currentScore < 70) {
            hapticManager.vibrateNoiseSpike()
            _uiState.update { it.copy(
                showHighNoiseAlert = true,
                highNoiseAlertMessage = "Your suitability score dropped from $lastAlertedScore to $currentScore"
            )}
            lastAlertedScore = currentScore
        }

        // Alert if sustained high noise
        if (db >= 75f) {
            if (highNoiseStartTime == 0L) highNoiseStartTime = System.currentTimeMillis()
            val highNoiseDuration = (System.currentTimeMillis() - highNoiseStartTime) / 1000L
            if (highNoiseDuration >= HIGH_NOISE_ALERT_THRESHOLD_SECONDS && !state.showHighNoiseAlert) {
                hapticManager.vibrateNoiseSpike()
                _uiState.update { it.copy(
                    showHighNoiseAlert = true,
                    highNoiseAlertMessage = "Your environment has been above your threshold for ${HIGH_NOISE_ALERT_THRESHOLD_SECONDS / 60} minutes"
                )}
            }
        } else {
            highNoiseStartTime = 0L
        }
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopCapture()
        captureJob?.cancel()
        timerJob?.cancel()
    }
}
