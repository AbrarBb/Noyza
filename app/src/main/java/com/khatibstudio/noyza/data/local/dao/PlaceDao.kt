package com.khatibstudio.noyza.data.local.dao

import androidx.room.*
import com.khatibstudio.noyza.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Query("SELECT * FROM places ORDER BY last_measured_at DESC")
    fun getAllPlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY best_suitability_score DESC")
    fun getPlacesByScore(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places ORDER BY average_db ASC")
    fun getPlacesByQuietest(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getPlaceById(id: Long): PlaceEntity?

    @Query("SELECT * FROM places WHERE category = :category ORDER BY best_suitability_score DESC")
    fun getPlacesByCategory(category: String): Flow<List<PlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity): Long

    @Update
    suspend fun updatePlace(place: PlaceEntity)

    @Delete
    suspend fun deletePlace(place: PlaceEntity)

    @Query("DELETE FROM places")
    suspend fun deleteAllPlaces()

    @Query("SELECT COUNT(*) FROM places")
    suspend fun getPlaceCount(): Int

    @Query("""
        UPDATE places SET 
            average_db = :avgDb, 
            best_suitability_score = MAX(best_suitability_score, :score),
            measurement_count = measurement_count + 1,
            last_measured_at = :timestamp
        WHERE id = :placeId
    """)
    suspend fun updatePlaceMeasurement(placeId: Long, avgDb: Float, score: Int, timestamp: Long)
}
