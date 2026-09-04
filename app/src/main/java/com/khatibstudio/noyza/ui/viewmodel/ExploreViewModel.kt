package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val places: List<Place> = emptyList(),
    val selectedActivity: ActivityType = ActivityType.STUDY,
    val sortIndex: Int = 0,
    val isAdsRemoved: Boolean = false
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val preferences: NoyZaPreferences
) : ViewModel() {

    private val _selectedActivity = MutableStateFlow(ActivityType.STUDY)
    private val _sortIndex = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.isAdsRemoved.collect { removed ->
                _uiState.update { it.copy(isAdsRemoved = removed) }
            }
        }
        viewModelScope.launch {
            preferences.defaultActivity.collect { activity ->
                _selectedActivity.value = activity
            }
        }
        viewModelScope.launch {
            combine(_sortIndex, _selectedActivity) { sortIndex, activity ->
                sortIndex to activity
            }.flatMapLatest { (sortIndex, activity) ->
                when (sortIndex) {
                    1 -> placeRepository.getPlacesByQuietest()
                    else -> placeRepository.getPlacesByScore()
                }
            }.collect { places ->
                _uiState.update { it.copy(
                    places = places,
                    selectedActivity = _selectedActivity.value,
                    sortIndex = _sortIndex.value
                )}
            }
        }
    }

    fun selectActivity(activity: ActivityType) {
        _selectedActivity.value = activity
    }

    fun selectSort(index: Int) {
        _sortIndex.value = index
    }
}
