package com.khatibstudio.noyza.data.repository

import com.khatibstudio.noyza.data.local.dao.PlaceDao
import com.khatibstudio.noyza.data.local.entity.PlaceEntity
import com.khatibstudio.noyza.domain.model.Place
import com.khatibstudio.noyza.domain.model.PlaceCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepository @Inject constructor(
    private val placeDao: PlaceDao
) {
    fun getAllPlaces(): Flow<List<Place>> =
        placeDao.getAllPlaces().map { it.map { e -> e.toDomain() } }

    fun getPlacesByScore(): Flow<List<Place>> =
        placeDao.getPlacesByScore().map { it.map { e -> e.toDomain() } }

    fun getPlacesByQuietest(): Flow<List<Place>> =
        placeDao.getPlacesByQuietest().map { it.map { e -> e.toDomain() } }

    fun getPlacesByCategory(category: PlaceCategory): Flow<List<Place>> =
        placeDao.getPlacesByCategory(category.name).map { it.map { e -> e.toDomain() } }

    suspend fun getPlaceById(id: Long): Place? =
        placeDao.getPlaceById(id)?.toDomain()

    suspend fun savePlace(place: Place): Long =
        placeDao.insertPlace(place.toEntity())

    suspend fun updatePlace(place: Place) =
        placeDao.updatePlace(place.toEntity())

    suspend fun deletePlace(place: Place) =
        placeDao.deletePlace(place.toEntity())

    suspend fun deleteAllPlaces() =
        placeDao.deleteAllPlaces()

    suspend fun getPlaceCount(): Int =
        placeDao.getPlaceCount()

    suspend fun updatePlaceMeasurement(placeId: Long, avgDb: Float, score: Int) =
        placeDao.updatePlaceMeasurement(placeId, avgDb, score, System.currentTimeMillis())

    private fun PlaceEntity.toDomain() = Place(
        id = id,
        name = name,
        category = PlaceCategory.fromName(category),
        notes = notes,
        latitude = latitude,
        longitude = longitude,
        averageDb = averageDb,
        bestSuitabilityScore = bestSuitabilityScore,
        measurementCount = measurementCount,
        lastMeasuredAt = lastMeasuredAt,
        createdAt = createdAt
    )

    private fun Place.toEntity() = PlaceEntity(
        id = id,
        name = name,
        category = category.name,
        notes = notes,
        latitude = latitude,
        longitude = longitude,
        averageDb = averageDb,
        bestSuitabilityScore = bestSuitabilityScore,
        measurementCount = measurementCount,
        lastMeasuredAt = lastMeasuredAt,
        createdAt = createdAt
    )
}
