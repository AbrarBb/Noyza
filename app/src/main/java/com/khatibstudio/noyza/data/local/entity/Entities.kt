package com.khatibstudio.noyza.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,           // PlaceCategory.name

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,

    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,

    @ColumnInfo(name = "average_db")
    val averageDb: Float = 0f,

    @ColumnInfo(name = "best_suitability_score")
    val bestSuitabilityScore: Int = 0,

    @ColumnInfo(name = "measurement_count")
    val measurementCount: Int = 0,

    @ColumnInfo(name = "last_measured_at")
    val lastMeasuredAt: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "activity_type")
    val activityType: String,       // ActivityType.name

    @ColumnInfo(name = "place_id")
    val placeId: Long? = null,

    @ColumnInfo(name = "place_name")
    val placeName: String? = null,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long = 0L,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,

    @ColumnInfo(name = "average_db")
    val averageDb: Float = 0f,

    @ColumnInfo(name = "minimum_db")
    val minimumDb: Float = 0f,

    @ColumnInfo(name = "maximum_db")
    val maximumDb: Float = 0f,

    @ColumnInfo(name = "stability_score")
    val stabilityScore: Float = 0f,

    @ColumnInfo(name = "suitability_score")
    val suitabilityScore: Int = 0,

    @ColumnInfo(name = "quiet_percent")
    val quietPercent: Float = 0f,

    @ColumnInfo(name = "moderate_percent")
    val moderatePercent: Float = 0f,

    @ColumnInfo(name = "loud_percent")
    val loudPercent: Float = 0f,

    @ColumnInfo(name = "very_loud_percent")
    val veryLoudPercent: Float = 0f,

    @ColumnInfo(name = "sample_count")
    val sampleCount: Int = 0
)

@Entity(tableName = "noise_samples")
data class NoiseSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "session_id", index = true)
    val sessionId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "estimated_db")
    val estimatedDb: Float
)
