package com.khatibstudio.noyza.data.local.dao

import androidx.room.*
import com.khatibstudio.noyza.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE start_time >= :fromTimestamp ORDER BY start_time DESC")
    fun getSessionsSince(fromTimestamp: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE activity_type = :activityType ORDER BY start_time DESC")
    fun getSessionsByActivity(activityType: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE place_id = :placeId ORDER BY start_time DESC")
    fun getSessionsByPlace(placeId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("""
        SELECT * FROM sessions 
        WHERE start_time >= :fromTimestamp AND start_time <= :toTimestamp
        ORDER BY start_time DESC
    """)
    fun getSessionsInRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<SessionEntity>>

    @Query("""
        SELECT AVG(average_db) as avg_db, AVG(suitability_score) as avg_score
        FROM sessions
        WHERE start_time >= :fromTimestamp
    """)
    suspend fun getAveragesince(fromTimestamp: Long): SessionAverages?

    @Query("""
        SELECT * FROM sessions 
        ORDER BY start_time DESC 
        LIMIT 30
    """)
    fun getRecentSessions(): Flow<List<SessionEntity>>
}

data class SessionAverages(
    val avg_db: Float?,
    val avg_score: Float?
)
