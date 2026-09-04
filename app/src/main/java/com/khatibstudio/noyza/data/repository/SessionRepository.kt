package com.khatibstudio.noyza.data.repository

import com.khatibstudio.noyza.data.local.dao.NoiseSampleDao
import com.khatibstudio.noyza.data.local.dao.SessionDao
import com.khatibstudio.noyza.data.local.entity.NoiseSampleEntity
import com.khatibstudio.noyza.data.local.entity.SessionEntity
import com.khatibstudio.noyza.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val noiseSampleDao: NoiseSampleDao
) {
    fun getAllSessions(): Flow<List<Session>> =
        sessionDao.getAllSessions().map { it.map { e -> e.toDomain() } }

    fun getRecentSessions(): Flow<List<Session>> =
        sessionDao.getRecentSessions().map { it.map { e -> e.toDomain() } }

    fun getSessionsSince(days: Int): Flow<List<Session>> {
        val from = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return sessionDao.getSessionsSince(from).map { it.map { e -> e.toDomain() } }
    }

    fun getSessionsByActivity(activityName: String): Flow<List<Session>> =
        sessionDao.getSessionsByActivity(activityName).map { it.map { e -> e.toDomain() } }

    fun getSessionsByPlace(placeId: Long): Flow<List<Session>> =
        sessionDao.getSessionsByPlace(placeId).map { it.map { e -> e.toDomain() } }

    suspend fun getSessionById(id: Long): Session? =
        sessionDao.getSessionById(id)?.toDomain()

    suspend fun saveSession(session: Session): Long {
        return sessionDao.insertSession(session.toEntity())
    }

    suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session.toEntity())
    }

    suspend fun deleteAllData() {
        sessionDao.deleteAllSessions()
        noiseSampleDao.deleteAllSamples()
    }

    suspend fun saveSamples(sessionId: Long, dbValues: List<Float>) {
        val now = System.currentTimeMillis()
        val samples = dbValues.mapIndexed { index, db ->
            NoiseSampleEntity(
                sessionId = sessionId,
                timestamp = now - ((dbValues.size - index) * 1000L),
                estimatedDb = db
            )
        }
        noiseSampleDao.insertSamples(samples)
    }

    suspend fun getSamplesForSession(sessionId: Long): List<Float> =
        noiseSampleDao.getDownsampledForSession(sessionId, 300).map { it.estimatedDb }

    private fun SessionEntity.toDomain() = Session(
        id = id,
        activityType = com.khatibstudio.noyza.domain.model.ActivityType.fromName(activityType),
        placeId = placeId,
        placeName = placeName,
        startTime = startTime,
        endTime = endTime,
        durationSeconds = durationSeconds,
        averageDb = averageDb,
        minimumDb = minimumDb,
        maximumDb = maximumDb,
        stabilityScore = stabilityScore,
        suitabilityScore = suitabilityScore,
        quietPercent = quietPercent,
        moderatePercent = moderatePercent,
        loudPercent = loudPercent,
        veryLoudPercent = veryLoudPercent,
        sampleCount = sampleCount
    )

    private fun Session.toEntity() = SessionEntity(
        id = id,
        activityType = activityType.name,
        placeId = placeId,
        placeName = placeName,
        startTime = startTime,
        endTime = endTime,
        durationSeconds = durationSeconds,
        averageDb = averageDb,
        minimumDb = minimumDb,
        maximumDb = maximumDb,
        stabilityScore = stabilityScore,
        suitabilityScore = suitabilityScore,
        quietPercent = quietPercent,
        moderatePercent = moderatePercent,
        loudPercent = loudPercent,
        veryLoudPercent = veryLoudPercent,
        sampleCount = sampleCount
    )
}
