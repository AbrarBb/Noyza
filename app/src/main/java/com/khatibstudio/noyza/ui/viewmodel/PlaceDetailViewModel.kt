package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.engine.SuitabilityEngine
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaceDetailUiState(
    val place: Place? = null,
    val samples: List<Float> = emptyList(),
    val activityCompatibility: Map<ActivityType, Int> = emptyMap()
)

@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val sessionRepository: SessionRepository,
    private val suitabilityEngine: SuitabilityEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceDetailUiState())
    val uiState: StateFlow<PlaceDetailUiState> = _uiState.asStateFlow()

    fun loadPlace(placeId: Long) {
        viewModelScope.launch {
            val place = placeRepository.getPlaceById(placeId) ?: return@launch
            _uiState.update { it.copy(place = place) }

            // Load noise samples from most recent session at this place
            val sessions = sessionRepository.getSessionsByPlace(placeId).first()
            val latestSession = sessions.firstOrNull()
            if (latestSession != null) {
                val samples = sessionRepository.getSamplesForSession(latestSession.id)
                _uiState.update { it.copy(samples = samples) }

                // Calculate activity compatibility using average dB
                val avg = place.averageDb
                val peak = latestSession.maximumDb
                val min = latestSession.minimumDb
                val stability = latestSession.stabilityScore
                val loudTime = latestSession.loudPercent + latestSession.veryLoudPercent

                val compatibility = ActivityType.entries.associate { activity ->
                    val result = suitabilityEngine.calculate(
                        activity = activity,
                        averageDb = avg,
                        peakDb = peak,
                        minimumDb = min,
                        stabilityPercent = stability,
                        loudTimePercent = loudTime
                    )
                    activity to result.score
                }
                _uiState.update { it.copy(activityCompatibility = compatibility) }
            }
        }
    }
}
