package com.khatibstudio.noyza.data.local.dao

import androidx.room.*
import com.khatibstudio.noyza.data.local.entity.NoiseSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoiseSampleDao {

    @Query("SELECT * FROM noise_samples WHERE session_id = :sessionId ORDER BY timestamp ASC")
    fun getSamplesForSession(sessionId: Long): Flow<List<NoiseSampleEntity>>

    @Query("SELECT * FROM noise_samples WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSessionSync(sessionId: Long): List<NoiseSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: NoiseSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<NoiseSampleEntity>)

    @Query("DELETE FROM noise_samples WHERE session_id = :sessionId")
    suspend fun deleteSamplesForSession(sessionId: Long)

    @Query("DELETE FROM noise_samples")
    suspend fun deleteAllSamples()

    /**
     * Downsample for graph display — returns at most [limit] evenly spaced samples.
     * This prevents memory issues with very long sessions.
     */
    @Query("""
        SELECT * FROM noise_samples 
        WHERE session_id = :sessionId 
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getDownsampledForSession(sessionId: Long, limit: Int = 300): List<NoiseSampleEntity>
}
