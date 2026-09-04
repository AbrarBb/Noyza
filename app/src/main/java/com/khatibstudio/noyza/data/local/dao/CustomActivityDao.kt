package com.khatibstudio.noyza.data.local.dao

import androidx.room.*
import com.khatibstudio.noyza.data.local.entity.CustomActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomActivityDao {

    @Query("SELECT * FROM custom_activities ORDER BY created_at ASC")
    fun getAllCustomActivities(): Flow<List<CustomActivityEntity>>

    @Query("SELECT * FROM custom_activities WHERE id = :id")
    suspend fun getCustomActivityById(id: Long): CustomActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomActivity(activity: CustomActivityEntity): Long

    @Delete
    suspend fun deleteCustomActivity(activity: CustomActivityEntity)

    @Query("DELETE FROM custom_activities")
    suspend fun deleteAllCustomActivities()
}
