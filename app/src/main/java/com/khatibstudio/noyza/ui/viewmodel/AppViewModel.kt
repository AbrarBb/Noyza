package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.domain.model.ActivityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: NoyZaPreferences
) : ViewModel() {

    val isOnboardingComplete: StateFlow<Boolean> = preferences.isOnboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPremium: StateFlow<Boolean> = preferences.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAdsRemoved: StateFlow<Boolean> = preferences.isAdsRemoved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultActivity: StateFlow<ActivityType> = preferences.defaultActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActivityType.STUDY)

    val calibrationOffset: StateFlow<Float> = preferences.calibrationOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
}
