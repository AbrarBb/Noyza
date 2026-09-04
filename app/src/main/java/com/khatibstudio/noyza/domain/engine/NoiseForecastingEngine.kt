package com.khatibstudio.noyza.domain.engine

import com.khatibstudio.noyza.domain.model.Session
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class HourlyForecastSlot(
    val hourOfDay: Int,          // 0..23
    val formattedHour: String,   // "9 AM", "10 AM", etc.
    val expectedDb: Float,
    val isQuietest: Boolean = false,
    val isPeak: Boolean = false,
    val sessionCount: Int = 0
)

data class PlaceScheduleForecast(
    val quietestWindow: String,         // e.g. "9:00 AM – 11:30 AM"
    val quietestAverageDb: Float,
    val peakWindow: String,             // e.g. "1:00 PM – 3:30 PM"
    val peakAverageDb: Float,
    val recommendationSummary: String,
    val hourlySlots: List<HourlyForecastSlot>
)

/**
 * Predictive engine that aggregates historical noise measurements across times of day
 * to forecast future quiet windows and ideal visit times.
 */
@Singleton
class NoiseForecastingEngine @Inject constructor() {

    // Standard baseline diurnal urban noise curve (used as an intelligent Bayesian prior)
    private val baselineHourlyDb = floatArrayOf(
        36f, 34f, 33f, 33f, 35f, 40f, 48f, 56f, 58f, 50f, 48f, 53f,
        58f, 62f, 56f, 54f, 57f, 63f, 61f, 58f, 53f, 48f, 42f, 38f
    )

    /**
     * Compute predictive schedule forecast for a list of historical sessions.
     */
    fun forecastForSessions(sessions: List<Session>): PlaceScheduleForecast {
        val hourSums = FloatArray(24) { 0f }
        val hourCounts = IntArray(24) { 0 }

        val cal = Calendar.getInstance()
        sessions.forEach { session ->
            cal.timeInMillis = session.startTime
            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourSums[hour] += session.averageDb
            hourCounts[hour]++
        }

        // Combine empirical session data with diurnal prior
        val hourlySlots = (0..23).map { hour ->
            val expected = if (hourCounts[hour] > 0) {
                val measuredAvg = hourSums[hour] / hourCounts[hour]
                // Weighted blend: 75% measured, 25% diurnal prior
                (measuredAvg * 0.75f) + (baselineHourlyDb[hour] * 0.25f)
            } else {
                baselineHourlyDb[hour]
            }

            val formatted = when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }

            HourlyForecastSlot(
                hourOfDay = hour,
                formattedHour = formatted,
                expectedDb = expected,
                sessionCount = hourCounts[hour]
            )
        }

        // Active daytime hours (07:00 to 22:00) for recommendation
        val daytimeSlots = hourlySlots.filter { it.hourOfDay in 7..21 }

        val quietestSlot = daytimeSlots.minByOrNull { it.expectedDb } ?: hourlySlots[9]
        val peakSlot = daytimeSlots.maxByOrNull { it.expectedDb } ?: hourlySlots[13]

        val updatedSlots = hourlySlots.map { slot ->
            slot.copy(
                isQuietest = slot.hourOfDay == quietestSlot.hourOfDay,
                isPeak = slot.hourOfDay == peakSlot.hourOfDay
            )
        }

        val quietEndHour = (quietestSlot.hourOfDay + 2).coerceAtMost(23)
        val quietEndFormatted = if (quietEndHour < 12) "$quietEndHour:00 AM" else "${if (quietEndHour == 12) 12 else quietEndHour - 12}:00 PM"
        val quietWindow = "${quietestSlot.formattedHour} – $quietEndFormatted"

        val peakEndHour = (peakSlot.hourOfDay + 2).coerceAtMost(23)
        val peakEndFormatted = if (peakEndHour < 12) "$peakEndHour:00 AM" else "${if (peakEndHour == 12) 12 else peakEndHour - 12}:00 PM"
        val peakWindow = "${peakSlot.formattedHour} – $peakEndFormatted"

        val summary = when {
            quietestSlot.hourOfDay in 7..11 -> "Typically quietest in the morning before lunch hours."
            quietestSlot.hourOfDay in 14..16 -> "Typically quietest during the mid-afternoon lull."
            else -> "Quietest during evening wind-down hours."
        }

        return PlaceScheduleForecast(
            quietestWindow = quietWindow,
            quietestAverageDb = quietestSlot.expectedDb,
            peakWindow = peakWindow,
            peakAverageDb = peakSlot.expectedDb,
            recommendationSummary = summary,
            hourlySlots = updatedSlots
        )
    }
}
