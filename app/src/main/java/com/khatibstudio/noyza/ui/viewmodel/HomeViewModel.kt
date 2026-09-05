package com.khatibstudio.noyza.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.audio.AudioEngine
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.engine.SuitabilityEngine
import com.khatibstudio.noyza.domain.model.*
import com.khatibstudio.noyza.audio.SoundProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

import com.khatibstudio.noyza.data.local.entity.CustomActivityEntity
import com.khatibstudio.noyza.data.local.entity.toProfile
import com.khatibstudio.noyza.data.repository.CustomActivityRepository
import com.khatibstudio.noyza.domain.model.ActivityProfile
import com.khatibstudio.noyza.domain.model.CustomActivityProfile

data class HomeUiState(
    val currentDb: Float = 0f,
    val averageDb: Float = 0f,
    val peakDb: Float = 0f,
    val minimumDb: Float = Float.MAX_VALUE,
    val noiseLevel: NoiseLevel = NoiseLevel.QUIET,
    val selectedActivity: ActivityProfile = ActivityType.STUDY,
    val suitabilityResult: SuitabilityResult = SuitabilityResult(),
    val durationSeconds: Long = 0L,
    val isActive: Boolean = false,
    val hasMicPermission: Boolean = false,
    val recentSessions: List<Session> = emptyList(),
    val savedPlaces: List<Place> = emptyList(),
    val greeting: String = "Good morning",
    val isPremium: Boolean = false,
    val isAdsRemoved: Boolean = false,
    val dbSamples: List<Float> = emptyList(),    // for stability calculation
    val soundProfile: SoundProfile = SoundProfile(),
    val customActivities: List<CustomActivityProfile> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val audioEngine: AudioEngine,
    private val suitabilityEngine: SuitabilityEngine,
    private val preferences: NoyZaPreferences,
    private val sessionRepository: SessionRepository,
    private val placeRepository: PlaceRepository,
    private val customActivityRepository: CustomActivityRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Internal measurement accumulators
    private var dbSamplesList = mutableListOf<Float>()
    private var measurementStartTime = 0L
    private var quickMeasureJob: Job? = null
    private var timerJob: Job? = null

    init {
        checkMicPermission()
        loadInitialData()
        observeAudio()
        updateGreeting()
    }

    private fun checkMicPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasMicPermission = hasPermission) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Default activity
            preferences.defaultActivity.collect { activity ->
                _uiState.update { it.copy(selectedActivity = activity) }
            }
        }
        viewModelScope.launch {
            preferences.isPremium.collect { isPremium ->
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
        viewModelScope.launch {
            preferences.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
        viewModelScope.launch {
            sessionRepository.getRecentSessions().collect { sessions ->
                _uiState.update { it.copy(recentSessions = sessions.take(3)) }
            }
        }
        viewModelScope.launch {
            placeRepository.getPlacesByScore().collect { places ->
                _uiState.update { it.copy(savedPlaces = places.take(5)) }
            }
        }
        viewModelScope.launch {
            customActivityRepository.getAllCustomActivities().collect { list ->
                _uiState.update { it.copy(customActivities = list.map { e -> e.toProfile() }) }
            }
        }
    }

    private var lastSuitabilityCalculationTime = 0L
    private var smoothedSuitabilityScore: Float? = null

    private fun observeAudio() {
        viewModelScope.launch {
            preferences.calibrationOffset.collect { offset ->
                audioEngine.setCalibrationOffset(offset)
            }
        }

        viewModelScope.launch {
            audioEngine.soundProfile.collect { profile ->
                _uiState.update { it.copy(soundProfile = profile) }
            }
        }

        viewModelScope.launch {
            audioEngine.smoothedDb.collect { db ->
                if (!_uiState.value.isActive) return@collect

                val state = _uiState.value
                val newSamples = (state.dbSamples + db).takeLast(300) // Keep max 300 samples
                val newAvg = newSamples.average().toFloat()
                val newPeak = max(state.peakDb, db)
                val newMin = if (state.minimumDb == Float.MAX_VALUE) db
                else min(state.minimumDb, db)

                val now = System.currentTimeMillis()
                val shouldUpdateSuitability = (now - lastSuitabilityCalculationTime) >= 500L || state.suitabilityResult.score == 0

                val suitability = if (shouldUpdateSuitability) {
                    lastSuitabilityCalculationTime = now
                    val stability = suitabilityEngine.calculateStability(newSamples)
                    val loudTime = newSamples.count { it >= 68f } / newSamples.size.toFloat() * 100f

                    val rawSuitability = suitabilityEngine.calculate(
                        activity = state.selectedActivity,
                        averageDb = newAvg,
                        peakDb = newPeak,
                        minimumDb = newMin,
                        stabilityPercent = stability,
                        loudTimePercent = loudTime,
                        samples = newSamples,
                        soundCharacter = state.soundProfile.character,
                        previousState = state.suitabilityResult.state,
                        previousRecommendation = state.suitabilityResult.recommendation
                    )

                    // Smooth score with EMA so transitions are gradual and calm
                    val currentSmoothed = smoothedSuitabilityScore ?: rawSuitability.score.toFloat()
                    val nextSmoothed = (0.25f * rawSuitability.score) + (0.75f * currentSmoothed)
                    smoothedSuitabilityScore = nextSmoothed

                    val finalScoreInt = nextSmoothed.toInt().coerceIn(0, 100)
                    val stableState = suitabilityEngine.resolveStateWithHysteresis(finalScoreInt, state.suitabilityResult.state)
                    val stableRecommendation = suitabilityEngine.resolveRecommendationWithHysteresis(finalScoreInt, state.suitabilityResult.recommendation)

                    rawSuitability.copy(
                        score = finalScoreInt,
                        state = stableState,
                        recommendation = stableRecommendation
                    )
                } else {
                    state.suitabilityResult
                }

                _uiState.update { it.copy(
                    currentDb = db,
                    averageDb = newAvg,
                    peakDb = newPeak,
                    minimumDb = newMin,
                    noiseLevel = NoiseLevel.fromDb(db),
                    suitabilityResult = suitability,
                    dbSamples = newSamples
                )}
            }
        }
    }

    fun selectActivity(activity: ActivityProfile) {
        smoothedSuitabilityScore = null
        lastSuitabilityCalculationTime = 0L
        _uiState.update { it.copy(selectedActivity = activity) }
        if (activity is ActivityType) {
            viewModelScope.launch {
                preferences.setDefaultActivity(activity)
            }
        }
    }

    fun saveCustomActivity(activity: CustomActivityEntity) {
        viewModelScope.launch {
            val id = customActivityRepository.saveCustomActivity(activity)
            val profile = activity.copy(id = id).toProfile()
            selectActivity(profile)
        }
    }

    fun startLiveMonitoring() {
        if (!_uiState.value.hasMicPermission) return
        checkMicPermission()
        if (!_uiState.value.hasMicPermission) return

        smoothedSuitabilityScore = null
        lastSuitabilityCalculationTime = 0L
        dbSamplesList.clear()
        measurementStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(
            isActive = true,
            averageDb = 0f,
            peakDb = 0f,
            minimumDb = Float.MAX_VALUE,
            dbSamples = emptyList(),
            durationSeconds = 0L
        )}

        viewModelScope.launch(Dispatchers.IO) {
            audioEngine.startCapture()
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - measurementStartTime) / 1000L
                _uiState.update { it.copy(durationSeconds = elapsed) }
            }
        }
    }

    fun stopLiveMonitoring() {
        timerJob?.cancel()
        audioEngine.stopCapture()
        _uiState.update { it.copy(isActive = false) }
    }

    /**
     * Quick Measure: 15 second measurement, returns result.
     */
    fun startQuickMeasure(onComplete: (SuitabilityResult, Float, Float) -> Unit) {
        if (!_uiState.value.hasMicPermission) return

        val samples = mutableListOf<Float>()
        quickMeasureJob?.cancel()

        _uiState.update { it.copy(isActive = true) }

        quickMeasureJob = viewModelScope.launch {
            launch(Dispatchers.IO) { audioEngine.startCapture() }

            audioEngine.smoothedDb
                .take(150) // ~15 seconds at ~10 samples/sec
                .collect { db -> samples.add(db) }

            audioEngine.stopCapture()
            _uiState.update { it.copy(isActive = false) }

            if (samples.isNotEmpty()) {
                val avg = samples.average().toFloat()
                val peak = samples.max()
                val min = samples.min()
                val stability = suitabilityEngine.calculateStability(samples)
                val loudTime = samples.count { it >= 68f } / samples.size.toFloat() * 100f

                val result = suitabilityEngine.calculate(
                    activity = _uiState.value.selectedActivity,
                    averageDb = avg,
                    peakDb = peak,
                    minimumDb = min,
                    stabilityPercent = stability,
                    loudTimePercent = loudTime,
                    samples = samples,
                    soundCharacter = _uiState.value.soundProfile.character
                )
                onComplete(result, avg, peak)
            }
        }
    }

    fun cancelQuickMeasure() {
        quickMeasureJob?.cancel()
        audioEngine.stopCapture()
        _uiState.update { it.copy(isActive = false) }
    }

    private fun updateGreeting() {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        _uiState.update { it.copy(greeting = greeting) }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopCapture()
        timerJob?.cancel()
        quickMeasureJob?.cancel()
    }
}
