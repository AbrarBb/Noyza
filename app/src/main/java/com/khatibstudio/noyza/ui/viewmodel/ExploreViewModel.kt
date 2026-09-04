package com.khatibstudio.noyza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import com.khatibstudio.noyza.data.repository.PlaceRepository
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.Place
import com.khatibstudio.noyza.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreUiState(
    val places: List<Place> = emptyList(),
    val selectedActivity: ActivityType = ActivityType.STUDY,
    val sortIndex: Int = 0, // 0 = Best Match, 1 = Quietest, 2 = Nearby
    val isAdsRemoved: Boolean = false,
    val userCoordinates: Pair<Double, Double>? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val preferences: NoyZaPreferences,
    val locationHelper: LocationHelper
) : ViewModel() {

    private val _selectedActivity = MutableStateFlow(ActivityType.STUDY)
    private val _sortIndex = MutableStateFlow(0)
    private val _userCoordinates = MutableStateFlow<Pair<Double, Double>?>(null)
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

        // Fetch location for proximity sorting
        viewModelScope.launch {
            val coords = locationHelper.getCurrentCoordinates()
            _userCoordinates.value = coords
            _uiState.update { it.copy(userCoordinates = coords) }
        }

        viewModelScope.launch {
            combine(_sortIndex, _selectedActivity, _userCoordinates) { sortIndex, activity, coords ->
                Triple(sortIndex, activity, coords)
            }.flatMapLatest { (sortIndex, _, coords) ->
                when (sortIndex) {
                    1 -> placeRepository.getPlacesByQuietest()
                    2 -> placeRepository.getPlacesByScore().map { list ->
                        if (coords != null) {
                            list.sortedBy { place ->
                                if (place.latitude != null && place.longitude != null) {
                                    locationHelper.calculateDistanceMeters(
                                        coords.first, coords.second,
                                        place.latitude, place.longitude
                                    )
                                } else {
                                    Float.MAX_VALUE
                                }
                            }
                        } else {
                            list
                        }
                    }
                    else -> placeRepository.getPlacesByScore()
                }
            }.collect { places ->
                _uiState.update { it.copy(
                    places = places,
                    selectedActivity = _selectedActivity.value,
                    sortIndex = _sortIndex.value,
                    userCoordinates = _userCoordinates.value
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

    fun getDistanceLabel(place: Place): String? {
        val coords = _userCoordinates.value ?: return null
        val lat = place.latitude ?: return null
        val lng = place.longitude ?: return null
        val meters = locationHelper.calculateDistanceMeters(coords.first, coords.second, lat, lng)
        return locationHelper.formatDistance(meters)
    }
}
