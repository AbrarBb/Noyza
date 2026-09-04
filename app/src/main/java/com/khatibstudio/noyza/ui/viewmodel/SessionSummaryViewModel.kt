package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.model.Place
import com.khatibstudio.noyza.domain.model.PlaceCategory
import com.khatibstudio.noyza.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSummaryUiState(
    val session: Session? = null,
    val samples: List<Float> = emptyList(),
    val isAdsRemoved: Boolean = false
)

@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val placeRepository: PlaceRepository,
    private val preferences: NoyZaPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepository.getSessionById(sessionId)
            val samples = sessionRepository.getSamplesForSession(sessionId)
            _uiState.update { it.copy(session = session, samples = samples) }
        }
    }

    fun savePlace(
        sessionId: Long,
        name: String,
        category: PlaceCategory,
        notes: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val place = Place(
                name = name,
                category = category,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                averageDb = session.averageDb,
                bestSuitabilityScore = session.suitabilityScore,
                measurementCount = 1,
                lastMeasuredAt = session.endTime,
                createdAt = System.currentTimeMillis()
            )
            val placeId = placeRepository.savePlace(place)

            // Link session to place
            val updatedSession = session.copy(placeId = placeId, placeName = name)
            sessionRepository.saveSession(updatedSession)
        }
    }
}
