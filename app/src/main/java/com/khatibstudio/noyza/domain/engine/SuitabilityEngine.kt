package com.khatibstudio.noyza.domain.engine

import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.RecommendationType
import com.khatibstudio.noyza.domain.model.SuitabilityResult
import com.khatibstudio.noyza.domain.model.SuitabilityState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Suitability scoring engine.
 *
 * Weighted formula:
 *  - 40% → Average noise suitability (how close to ideal range)
 *  - 25% → Noise stability (standard deviation / variance penalty)
 *  - 20% → Peak noise behavior (how bad the worst moments were)
 *  - 15% → Sustained noise duration (time spent in poor ranges)
 *
 * All scores are transparent heuristics — NOT medical advice.
 * Scores represent app recommendations for environment suitability.
 */
@Singleton
class SuitabilityEngine @Inject constructor() {

    /**
     * Calculate suitability for a completed session or quick measure.
     *
     * @param activity         The selected activity
     * @param averageDb        Average estimated dB over the measurement period
     * @param peakDb           Maximum dB spike observed
     * @param minimumDb        Minimum dB observed
     * @param stabilityPercent 0–100, where 100 = perfectly stable (no spikes)
     * @param loudTimePercent  Percentage of time in Loud/VeryLoud range (0–100)
     * @param samples          Optional list of samples for distribution calculation
     */
    fun calculate(
        activity: ActivityType,
        averageDb: Float,
        peakDb: Float,
        minimumDb: Float,
        stabilityPercent: Float,
        loudTimePercent: Float,
        samples: List<Float> = emptyList()
    ): SuitabilityResult {

        // ── Component 1: Average noise suitability (40%) ──────────────────────
        val avgScore = scoreAverageNoise(averageDb, activity)

        // ── Component 2: Stability score (25%) ────────────────────────────────
        val stabilityScore = scoreStability(stabilityPercent, activity.stabilitySensitivity)

        // ── Component 3: Peak noise penalty (20%) ─────────────────────────────
        val peakScore = scorePeakNoise(peakDb, activity)

        // ── Component 4: Sustained exposure penalty (15%) ─────────────────────
        val sustainedScore = scoreSustainedExposure(loudTimePercent, activity.spikeSensitivity)

        // ── Weighted composite ────────────────────────────────────────────────
        val rawScore = (avgScore * 0.40f) + (stabilityScore * 0.25f) +
                (peakScore * 0.20f) + (sustainedScore * 0.15f)

        val finalScore = rawScore.toInt().coerceIn(0, 100)
        val state = SuitabilityState.fromScore(finalScore)

        // ── Distribution calculation ──────────────────────────────────────────
        val distribution = calculateDistribution(samples.ifEmpty {
            listOf(averageDb) // fallback when no samples
        })

        // ── Build result ──────────────────────────────────────────────────────
        return SuitabilityResult(
            score = finalScore,
            state = state,
            activity = activity,
            recommendation = recommendationType(finalScore),
            headline = buildHeadline(finalScore, state, activity),
            description = buildDescription(finalScore, state, activity, averageDb, stabilityPercent),
            stabilityPercent = stabilityPercent,
            quietPercent = distribution.first,
            moderatePercent = distribution.second,
            loudPercent = distribution.third,
            veryLoudPercent = distribution.fourth
        )
    }

    /**
     * Score how well the average dB fits the activity's ideal range.
     * Returns 0–100.
     */
    private fun scoreAverageNoise(averageDb: Float, activity: ActivityType): Float {
        return when {
            averageDb < activity.idealMinDb -> {
                // Below ideal minimum — for most activities this is still good (quieter is better)
                // Exception: Meeting/Conversation/Exercise which need some ambient energy
                if (activity == ActivityType.MEETING ||
                    activity == ActivityType.CONVERSATION ||
                    activity == ActivityType.EXERCISE
                ) {
                    // Too quiet for these activities
                    val deficit = activity.idealMinDb - averageDb
                    max(0f, 100f - (deficit * 2.5f))
                } else {
                    95f // Quiet is excellent for most activities
                }
            }
            averageDb <= activity.idealMaxDb -> {
                // Within ideal range — score 85–100 based on how centered
                val rangeCenter = (activity.idealMinDb + activity.idealMaxDb) / 2f
                val deviation = Math.abs(averageDb - rangeCenter)
                val rangeHalf = (activity.idealMaxDb - activity.idealMinDb) / 2f
                val normalizedDev = deviation / rangeHalf
                100f - (normalizedDev * 15f)
            }
            averageDb <= activity.acceptableMaxDb -> {
                // Acceptable range — linear penalty
                val overIdeal = averageDb - activity.idealMaxDb
                val acceptableRange = activity.acceptableMaxDb - activity.idealMaxDb
                val penalty = (overIdeal / acceptableRange) * 40f
                85f - penalty
            }
            else -> {
                // Poor range — exponential penalty
                val overAcceptable = averageDb - activity.acceptableMaxDb
                max(0f, 45f - (overAcceptable * 3f))
            }
        }
    }

    /**
     * Score stability. High stability sensitivity means instability is penalized more.
     */
    private fun scoreStability(stabilityPercent: Float, sensitivity: Float): Float {
        val instability = 100f - stabilityPercent
        val penaltyFactor = 1.0f + (sensitivity * 0.5f) // 1.0–1.5x
        return max(0f, 100f - (instability * penaltyFactor))
    }

    /**
     * Score peak behavior. A single loud spike should reduce score.
     */
    private fun scorePeakNoise(peakDb: Float, activity: ActivityType): Float {
        return when {
            peakDb <= activity.idealMaxDb -> 100f
            peakDb <= activity.acceptableMaxDb -> {
                val over = peakDb - activity.idealMaxDb
                val range = activity.acceptableMaxDb - activity.idealMaxDb
                90f - ((over / range) * 30f * activity.spikeSensitivity)
            }
            peakDb <= activity.poorThresholdDb + 10f -> {
                val over = peakDb - activity.acceptableMaxDb
                max(0f, 60f - (over * 3f * activity.spikeSensitivity))
            }
            else -> max(0f, 30f - ((peakDb - activity.poorThresholdDb) * 2f))
        }
    }

    /**
     * Penalize sustained time in loud ranges.
     */
    private fun scoreSustainedExposure(loudTimePercent: Float, sensitivity: Float): Float {
        if (loudTimePercent <= 5f) return 100f
        val penalty = (loudTimePercent - 5f) * (1.5f + sensitivity)
        return max(0f, 100f - penalty)
    }

    private fun recommendationType(score: Int): RecommendationType = when {
        score >= 70 -> RecommendationType.RECOMMENDED
        score >= 45 -> RecommendationType.CONSIDER_QUIETER
        else -> RecommendationType.NOT_IDEAL
    }

    private fun buildHeadline(score: Int, state: SuitabilityState, activity: ActivityType): String {
        return when (state) {
            SuitabilityState.EXCELLENT -> "Excellent for ${activity.displayName.lowercase()}"
            SuitabilityState.GOOD -> "Good place to ${activity.displayName.lowercase()}"
            SuitabilityState.MODERATE -> "Acceptable for ${activity.displayName.lowercase()}"
            SuitabilityState.POOR -> "Not ideal for ${activity.displayName.lowercase()}"
            SuitabilityState.NOT_RECOMMENDED -> "Difficult environment for ${activity.displayName.lowercase()}"
        }
    }

    private fun buildDescription(
        score: Int,
        state: SuitabilityState,
        activity: ActivityType,
        averageDb: Float,
        stabilityPercent: Float
    ): String {
        val stability = when {
            stabilityPercent >= 90f -> "very stable"
            stabilityPercent >= 75f -> "relatively stable"
            stabilityPercent >= 55f -> "somewhat variable"
            else -> "quite variable"
        }

        return when (state) {
            SuitabilityState.EXCELLENT ->
                "Low and $stability background noise. Great environment for ${activity.displayName.lowercase()}."
            SuitabilityState.GOOD ->
                "The environment is $stability and mostly within a good range for ${activity.displayName.lowercase()}."
            SuitabilityState.MODERATE ->
                "Noise is acceptable but $stability. You may notice occasional distractions."
            SuitabilityState.POOR ->
                "Noise levels are higher than ideal for ${activity.displayName.lowercase()}. Consider finding a quieter spot."
            SuitabilityState.NOT_RECOMMENDED ->
                "This environment may make ${activity.displayName.lowercase()} significantly more difficult."
        }
    }

    /**
     * Calculate percentage time in each noise category.
     * Returns (quietPercent, moderatePercent, loudPercent, veryLoudPercent)
     */
    private fun calculateDistribution(samples: List<Float>): Quadruple<Float, Float, Float, Float> {
        if (samples.isEmpty()) return Quadruple(100f, 0f, 0f, 0f)

        var quiet = 0
        var moderate = 0
        var loud = 0
        var veryLoud = 0

        samples.forEach { db ->
            when {
                db < 55f -> quiet++
                db < 68f -> moderate++
                db < 80f -> loud++
                else -> veryLoud++
            }
        }

        val total = samples.size.toFloat()
        return Quadruple(
            quiet / total * 100f,
            moderate / total * 100f,
            loud / total * 100f,
            veryLoud / total * 100f
        )
    }

    /**
     * Quick stability estimate from a list of dB samples.
     * Returns 0–100, where 100 = perfectly stable.
     */
    fun calculateStability(samples: List<Float>): Float {
        if (samples.size < 2) return 100f
        val mean = samples.average().toFloat()
        val variance = samples.map { (it - mean).pow(2) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()
        // Normalize: stdDev of 0 = 100% stable, stdDev of 20 = 0% stable
        return max(0f, 100f - (stdDev * 5f))
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
