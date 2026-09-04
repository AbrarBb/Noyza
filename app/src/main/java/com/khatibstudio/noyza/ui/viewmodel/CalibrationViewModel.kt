package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val preferences: NoyZaPreferences
) : ViewModel() {

    val calibrationOffset: StateFlow<Float> = preferences.calibrationOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun setOffset(offset: Float) {
        viewModelScope.launch {
            preferences.setCalibrationOffset(offset)
        }
    }
}
