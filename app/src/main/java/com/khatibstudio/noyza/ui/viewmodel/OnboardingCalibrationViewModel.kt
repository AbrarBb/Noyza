package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.audio.AudioEngine
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalibrationStepState(
    val isSampling: Boolean = false,
    val sampleProgress: Float = 0f,
    val currentDb: Float = 30f,
    val measuredAmbientDb: Float? = null,
    val selectedOffset: Float = 0f,
    val isComplete: Boolean = false
)

@HiltViewModel
class OnboardingCalibrationViewModel @Inject constructor(
    private val audioEngine: AudioEngine,
    private val preferences: NoyZaPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationStepState())
    val state: StateFlow<CalibrationStepState> = _state.asStateFlow()

    private var samplingJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.calibrationOffset.collect { offset ->
                _state.update { it.copy(selectedOffset = offset) }
            }
        }
    }

    fun start3SecondBaselineCheck() {
        if (_state.value.isSampling) return
        samplingJob?.cancel()

        samplingJob = viewModelScope.launch {
            _state.update { it.copy(isSampling = true, sampleProgress = 0f) }
            val samples = mutableListOf<Float>()

            val captureJob = launch {
                audioEngine.startCapture()
            }

            val collectJob = launch {
                audioEngine.smoothedDb.collect { db ->
                    if (db > 10f) {
                        samples.add(db)
                        _state.update { it.copy(currentDb = db) }
                    }
                }
            }

            // 3-second sampling with 30 progress steps
            val totalSteps = 30
            for (step in 1..totalSteps) {
                delay(100)
                _state.update { it.copy(sampleProgress = step.toFloat() / totalSteps) }
            }

            collectJob.cancel()
            audioEngine.stopCapture()
            captureJob.cancel()

            val avgDb = if (samples.isNotEmpty()) samples.average().toFloat() else 42f
            _state.update {
                it.copy(
                    isSampling = false,
                    measuredAmbientDb = avgDb,
                    isComplete = true
                )
            }
        }
    }

    fun selectPreset(offset: Float) {
        _state.update { it.copy(selectedOffset = offset.coerceIn(AudioEngine.MIN_CALIBRATION_OFFSET, AudioEngine.MAX_CALIBRATION_OFFSET)) }
    }

    fun saveCalibrationAndFinish(onDone: () -> Unit) {
        viewModelScope.launch {
            audioEngine.stopCapture()
            preferences.setCalibrationOffset(_state.value.selectedOffset)
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopCapture()
    }
}
