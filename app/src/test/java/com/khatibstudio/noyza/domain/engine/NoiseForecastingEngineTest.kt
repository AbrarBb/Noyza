package com.khatibstudio.noyza.domain.engine

import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.Session
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class NoiseForecastingEngineTest {

    private lateinit var engine: NoiseForecastingEngine

    @Before
    fun setup() {
        engine = NoiseForecastingEngine()
    }

    @Test
    fun `empty sessions list returns valid diurnal prior forecast`() {
        val forecast = engine.forecastForSessions(emptyList())

        assertNotNull(forecast)
        assertEquals(24, forecast.hourlySlots.size)
        assertTrue(forecast.quietestWindow.isNotEmpty())
        assertTrue(forecast.peakWindow.isNotEmpty())
        assertTrue(forecast.recommendationSummary.isNotEmpty())
        assertTrue(forecast.quietestAverageDb < forecast.peakAverageDb)
    }

    @Test
    fun `sessions with loud afternoon noise correctly adjust peak window`() {
        val cal = Calendar.getInstance()
        val sessions = mutableListOf<Session>()

        // Simulate 5 loud sessions at 2 PM (hour 14)
        for (i in 0 until 5) {
            cal.set(Calendar.HOUR_OF_DAY, 14)
            sessions.add(
                Session(
                    id = i.toLong(),
                    activityType = ActivityType.STUDY,
                    startTime = cal.timeInMillis,
                    endTime = cal.timeInMillis + 30 * 60 * 1000,
                    durationSeconds = 1800,
                    averageDb = 85f,
                    maximumDb = 95f,
                    minimumDb = 75f,
                    suitabilityScore = 40,
                    stabilityScore = 70f
                )
            )
        }

        // Simulate 5 quiet sessions at 9 AM (hour 9)
        for (i in 5 until 10) {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            sessions.add(
                Session(
                    id = i.toLong(),
                    activityType = ActivityType.STUDY,
                    startTime = cal.timeInMillis,
                    endTime = cal.timeInMillis + 30 * 60 * 1000,
                    durationSeconds = 1800,
                    averageDb = 35f,
                    maximumDb = 42f,
                    minimumDb = 30f,
                    suitabilityScore = 95,
                    stabilityScore = 95f
                )
            )
        }

        val forecast = engine.forecastForSessions(sessions)

        val slot9am = forecast.hourlySlots.first { it.hourOfDay == 9 }
        val slot2pm = forecast.hourlySlots.first { it.hourOfDay == 14 }

        assertTrue("Expected 9 AM to be quieter than 2 PM", slot9am.expectedDb < slot2pm.expectedDb)
        assertTrue("Expected 2 PM to have sessionCount == 5", slot2pm.sessionCount == 5)
        assertTrue("Expected 9 AM to have sessionCount == 5", slot9am.sessionCount == 5)
    }

    @Test
    fun `hourly slots have valid formatted labels`() {
        val forecast = engine.forecastForSessions(emptyList())
        val slot0 = forecast.hourlySlots.first { it.hourOfDay == 0 }
        val slot12 = forecast.hourlySlots.first { it.hourOfDay == 12 }
        val slot18 = forecast.hourlySlots.first { it.hourOfDay == 18 }

        assertEquals("12 AM", slot0.formattedHour)
        assertEquals("12 PM", slot12.formattedHour)
        assertEquals("6 PM", slot18.formattedHour)
    }
}
