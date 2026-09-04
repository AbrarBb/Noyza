package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HistoryUiState(
    val groupedSessions: Map<String, List<Session>> = emptyMap(),
    val selectedPeriodIndex: Int = 0,
    val isAdsRemoved: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferences: NoyZaPreferences
) : ViewModel() {

    private val _selectedPeriodIndex = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
        viewModelScope.launch {
            _selectedPeriodIndex.flatMapLatest { periodIndex ->
                when (periodIndex) {
                    0 -> sessionRepository.getSessionsSince(1) // Today
                    1 -> sessionRepository.getSessionsSince(7)
                    2 -> sessionRepository.getSessionsSince(30)
                    else -> sessionRepository.getAllSessions()
                }
            }.collect { sessions ->
                val grouped = groupSessionsByDate(sessions)
                _uiState.update { it.copy(
                    groupedSessions = grouped,
                    selectedPeriodIndex = _selectedPeriodIndex.value
                )}
            }
        }
    }

    fun selectPeriod(index: Int) {
        _selectedPeriodIndex.value = index
    }

    private fun groupSessionsByDate(sessions: List<Session>): Map<String, List<Session>> {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val sdf = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        return sessions.groupBy { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                else -> sdf.format(Date(session.startTime))
            }
        }
    }

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
