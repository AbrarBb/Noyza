package com.khatibstudio.noyza.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_activities")
data class CustomActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String = "Star",

    @ColumnInfo(name = "ideal_min_db")
    val idealMinDb: Float = 40f,

    @ColumnInfo(name = "ideal_max_db")
    val idealMaxDb: Float = 60f,

    @ColumnInfo(name = "acceptable_max_db")
    val acceptableMaxDb: Float = 70f,

    @ColumnInfo(name = "spike_sensitivity")
    val spikeSensitivity: Float = 1.0f,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

fun CustomActivityEntity.toProfile(): com.khatibstudio.noyza.domain.model.CustomActivityProfile =
    com.khatibstudio.noyza.domain.model.CustomActivityProfile(
        id = id,
        displayName = name,
        iconName = iconName,
        idealMinDb = idealMinDb,
        idealMaxDb = idealMaxDb,
        acceptableMaxDb = acceptableMaxDb,
        spikeSensitivity = spikeSensitivity,
        stabilitySensitivity = 0.85f
    )
