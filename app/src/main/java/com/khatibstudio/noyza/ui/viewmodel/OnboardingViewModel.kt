package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.domain.model.ActivityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: NoyZaPreferences
) : ViewModel() {

    fun setDefaultActivity(activity: ActivityType) {
        viewModelScope.launch {
            preferences.setDefaultActivity(activity)
        }
    }

    fun completeOnboarding(highNoiseEnabled: Boolean, dailySummaryEnabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotifHighNoise(highNoiseEnabled)
            preferences.setNotifDailySummary(dailySummaryEnabled)
            preferences.setOnboardingComplete()
        }
    }
}
