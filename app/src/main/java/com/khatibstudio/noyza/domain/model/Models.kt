package com.khatibstudio.noyza.domain.model

import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.NoiseLevel
import com.khatibstudio.noyza.domain.model.SuitabilityState
import com.khatibstudio.noyza.domain.model.RecommendationType

/**
 * Live measurement state emitted by the audio engine.
 */
data class MeasurementState(
    val currentDb: Float = 0f,
    val smoothedDb: Float = 0f,
    val averageDb: Float = 0f,
    val peakDb: Float = 0f,
    val minimumDb: Float = Float.MAX_VALUE,
    val noiseLevel: NoiseLevel = NoiseLevel.QUIET,
    val isActive: Boolean = false,
    val durationSeconds: Long = 0L,
    val sampleCount: Int = 0
)

/**
 * Suitability result for a given activity and measurement state.
 */
data class SuitabilityResult(
    val score: Int = 0,                    // 0–100
    val state: SuitabilityState = SuitabilityState.MODERATE,
    val activity: ActivityType = ActivityType.STUDY,
    val recommendation: RecommendationType = RecommendationType.RECOMMENDED,
    val headline: String = "",
    val description: String = "",
    val stabilityPercent: Float = 0f,
    val quietPercent: Float = 0f,
    val moderatePercent: Float = 0f,
    val loudPercent: Float = 0f,
    val veryLoudPercent: Float = 0f
)

/**
 * A saved place with aggregate measurements.
 */
data class Place(
    val id: Long = 0L,
    val name: String = "",
    val category: PlaceCategory = PlaceCategory.OTHER,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageDb: Float = 0f,
    val bestSuitabilityScore: Int = 0,
    val measurementCount: Int = 0,
    val lastMeasuredAt: Long = 0L,
    val createdAt: Long = 0L
)

/**
 * A completed session with full statistics.
 */
data class Session(
    val id: Long = 0L,
    val activityType: ActivityType = ActivityType.STUDY,
    val placeId: Long? = null,
    val placeName: String? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val averageDb: Float = 0f,
    val minimumDb: Float = 0f,
    val maximumDb: Float = 0f,
    val stabilityScore: Float = 0f,       // 0–100%
    val suitabilityScore: Int = 0,        // 0–100
    val quietPercent: Float = 0f,
    val moderatePercent: Float = 0f,
    val loudPercent: Float = 0f,
    val veryLoudPercent: Float = 0f,
    val sampleCount: Int = 0
)

/**
 * A time-series dB data point for graphing.
 */
data class NoiseSample(
    val id: Long = 0L,
    val sessionId: Long = 0L,
    val timestamp: Long = 0L,
    val estimatedDb: Float = 0f
)

/**
 * Place categories for filtering and display.
 */
enum class PlaceCategory(val displayName: String, val emoji: String) {
    LIBRARY("Library", "📚"),
    CAFE("Cafe", "☕"),
    OFFICE("Office", "🏢"),
    CLASSROOM("Classroom", "🏫"),
    HOME("Home", "🏠"),
    PARK("Park", "🌳"),
    GYM("Gym", "🏋"),
    RESTAURANT("Restaurant", "🍽"),
    OTHER("Other", "📍");

    companion object {
        fun fromName(name: String): PlaceCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/**
 * Activity compatibility score for a place.
 */
data class ActivityCompatibility(
    val activityType: ActivityType,
    val score: Int,
    val state: SuitabilityState
)

/**
 * Summary analytics for a time period.
 */
data class AnalyticsSummary(
    val periodLabel: String = "",
    val averageDb: Float = 0f,
    val bestDay: String = "",
    val noisiestDay: String = "",
    val averageSuitability: Int = 0,
    val totalDurationSeconds: Long = 0L,
    val quietDurationSeconds: Long = 0L,
    val moderateDurationSeconds: Long = 0L,
    val loudDurationSeconds: Long = 0L,
    val veryLoudDurationSeconds: Long = 0L,
    val sessionCount: Int = 0,
    val dailyAverageDb: Map<String, Float> = emptyMap(),
    val dailySuitability: Map<String, Int> = emptyMap()
)
