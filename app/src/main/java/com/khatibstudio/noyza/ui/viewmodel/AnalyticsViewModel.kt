package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.engine.NoiseForecastingEngine
import com.khatibstudio.noyza.domain.engine.PlaceScheduleForecast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsUiState(
    val avgDb: Float = 0f,
    val avgSuitability: Int = 0,
    val bestDay: String = "",
    val noisiestDay: String = "",
    val todayScore: Int = 0,
    val isPremium: Boolean = false,
    val isAdsRemoved: Boolean = false,
    val scheduleForecast: PlaceScheduleForecast? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferences: NoyZaPreferences,
    private val forecastingEngine: NoiseForecastingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isPremium.collect { premium ->
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            preferences.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
        viewModelScope.launch {
            sessionRepository.getSessionsSince(7).collect { sessions ->
                if (sessions.isEmpty()) return@collect

                val avgDb = sessions.map { it.averageDb }.average().toFloat()
                val avgSuit = sessions.map { it.suitabilityScore }.average().toInt()

                // Group by day to find best/noisiest
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val byDay = sessions.groupBy { dayFormat.format(Date(it.startTime)) }
                val bestDay = byDay.minByOrNull { (_, s) -> s.map { it.averageDb }.average() }?.key ?: ""
                val noisiestDay = byDay.maxByOrNull { (_, s) -> s.map { it.averageDb }.average() }?.key ?: ""

                // Today's score — average of today's sessions
                val todayScore = sessions
                    .filter { isToday(it.startTime) }
                    .map { it.suitabilityScore }
                    .average()
                    .let { if (it.isNaN()) 0 else it.toInt() }

                val forecast = forecastingEngine.forecastForSessions(sessions)

                _uiState.update { it.copy(
                    avgDb = avgDb,
                    avgSuitability = avgSuit,
                    bestDay = bestDay,
                    noisiestDay = noisiestDay,
                    todayScore = todayScore,
                    scheduleForecast = forecast
                )}
            }
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
    }
}
