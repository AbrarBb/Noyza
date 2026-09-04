package com.khatibstudio.noyza.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.data.repository.SessionRepository
import com.khatibstudio.noyza.domain.model.ActivityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val defaultActivity: ActivityType = ActivityType.STUDY,
    val calibrationOffset: Float = 0f,
    val notifHighNoise: Boolean = true,
    val notifDailySummary: Boolean = false,
    val isPremium: Boolean = false,
    val hapticAlertsEnabled: Boolean = true,
    val sensoryFriendlyMode: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferences: NoyZaPreferences,
    private val sessionRepository: SessionRepository,
    private val placeRepository: PlaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.defaultActivity,
                preferences.calibrationOffset,
                preferences.notifHighNoise,
                preferences.notifDailySummary,
                preferences.isPremium
            ) { activity, cal, noise, daily, premium ->
                ProfileUiState(
                    defaultActivity = activity,
                    calibrationOffset = cal,
                    notifHighNoise = noise,
                    notifDailySummary = daily,
                    isPremium = premium
                )
            }.combine(preferences.hapticAlertsEnabled) { state, haptics ->
                state.copy(hapticAlertsEnabled = haptics)
            }.combine(preferences.sensoryFriendlyMode) { state, sensory ->
                state.copy(sensoryFriendlyMode = sensory)
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setNotifHighNoise(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotifHighNoise(enabled)
    }

    fun setNotifDailySummary(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotifDailySummary(enabled)
    }

    fun setHapticAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setHapticAlertsEnabled(enabled)
    }

    fun setSensoryFriendlyMode(enabled: Boolean) = viewModelScope.launch {
        preferences.setSensoryFriendlyMode(enabled)
    }

    fun deleteAllData() = viewModelScope.launch {
        sessionRepository.deleteAllData()
        placeRepository.deleteAllPlaces()
    }

    fun exportCsv(context: Context) = viewModelScope.launch {
        val sessions = sessionRepository.getAllSessions().first()
        val csv = buildString {
            appendLine("id,activity,start_time,duration_seconds,average_db,peak_db,minimum_db,stability,suitability_score,place_name")
            sessions.forEach { s ->
                appendLine("${s.id},${s.activityType.displayName},${s.startTime},${s.durationSeconds},${s.averageDb},${s.maximumDb},${s.minimumDb},${s.stabilityScore},${s.suitabilityScore},${s.placeName ?: ""}")
            }
        }

        val file = File(context.cacheDir, "noyza_export.csv")
        file.writeText(csv)

        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Noyza Session Data Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Sessions"))
    }
}
