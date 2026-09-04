package com.khatibstudio.noyza.domain.model

/**
 * Represents all available activities in Noyza.
 * Each activity has defined dB thresholds for scoring.
 */
enum class ActivityType(
    val displayName: String,
    val emoji: String,
    val description: String,
    val idealMinDb: Float,
    val idealMaxDb: Float,
    val acceptableMaxDb: Float,
    val poorThresholdDb: Float,
    val spikeSensitivity: Float,   // 0.0 (low) to 1.0 (high)
    val stabilitySensitivity: Float // 0.0 (low) to 1.0 (high)
) {
    STUDY(
        displayName = "Study",
        emoji = "📚",
        description = "Focused academic study",
        idealMinDb = 35f,
        idealMaxDb = 55f,
        acceptableMaxDb = 70f,
        poorThresholdDb = 70f,
        spikeSensitivity = 0.8f,
        stabilitySensitivity = 0.85f
    ),
    DEEP_WORK(
        displayName = "Deep Work",
        emoji = "💻",
        description = "High-focus professional work",
        idealMinDb = 30f,
        idealMaxDb = 50f,
        acceptableMaxDb = 65f,
        poorThresholdDb = 65f,
        spikeSensitivity = 0.9f,
        stabilitySensitivity = 0.9f
    ),
    READING(
        displayName = "Reading",
        emoji = "📖",
        description = "Casual or intensive reading",
        idealMinDb = 35f,
        idealMaxDb = 55f,
        acceptableMaxDb = 65f,
        poorThresholdDb = 65f,
        spikeSensitivity = 0.7f,
        stabilitySensitivity = 0.8f
    ),
    RECORDING(
        displayName = "Recording",
        emoji = "🎙",
        description = "Audio/video recording",
        idealMinDb = 20f,
        idealMaxDb = 40f,
        acceptableMaxDb = 50f,
        poorThresholdDb = 50f,
        spikeSensitivity = 1.0f,
        stabilitySensitivity = 1.0f
    ),
    MEETING(
        displayName = "Meeting",
        emoji = "💬",
        description = "Online or in-person meetings",
        idealMinDb = 45f,
        idealMaxDb = 65f,
        acceptableMaxDb = 75f,
        poorThresholdDb = 75f,
        spikeSensitivity = 0.5f,
        stabilitySensitivity = 0.4f
    ),
    SLEEP(
        displayName = "Sleep",
        emoji = "😴",
        description = "Rest and sleeping",
        idealMinDb = 20f,
        idealMaxDb = 40f,
        acceptableMaxDb = 50f,
        poorThresholdDb = 50f,
        spikeSensitivity = 1.0f,
        stabilitySensitivity = 0.95f
    ),
    RELAX(
        displayName = "Relax",
        emoji = "🧘",
        description = "Relaxation and downtime",
        idealMinDb = 30f,
        idealMaxDb = 55f,
        acceptableMaxDb = 65f,
        poorThresholdDb = 65f,
        spikeSensitivity = 0.6f,
        stabilitySensitivity = 0.65f
    ),
    FOCUS(
        displayName = "Focus",
        emoji = "🎧",
        description = "General focused tasks",
        idealMinDb = 35f,
        idealMaxDb = 55f,
        acceptableMaxDb = 68f,
        poorThresholdDb = 68f,
        spikeSensitivity = 0.75f,
        stabilitySensitivity = 0.8f
    ),
    CONVERSATION(
        displayName = "Conversation",
        emoji = "🗣",
        description = "Casual conversation",
        idealMinDb = 50f,
        idealMaxDb = 70f,
        acceptableMaxDb = 80f,
        poorThresholdDb = 80f,
        spikeSensitivity = 0.3f,
        stabilitySensitivity = 0.3f
    ),
    EXERCISE(
        displayName = "Exercise",
        emoji = "🏋",
        description = "Physical exercise",
        idealMinDb = 55f,
        idealMaxDb = 80f,
        acceptableMaxDb = 90f,
        poorThresholdDb = 90f,
        spikeSensitivity = 0.2f,
        stabilitySensitivity = 0.2f
    );

    companion object {
        fun fromName(name: String): ActivityType =
            entries.firstOrNull { it.name == name } ?: STUDY
    }
}

/**
 * Noise level classification thresholds (estimated dB)
 */
enum class NoiseLevel(val label: String, val description: String) {
    QUIET("Quiet", "Very quiet environment"),
    MODERATELY_QUIET("Moderately Quiet", "Relatively quiet with minor background noise"),
    MODERATE("Moderate", "Noticeable background noise"),
    LOUD("Loud", "Significantly noisy environment"),
    VERY_LOUD("Very Loud", "Very noisy — difficult to hear");

    companion object {
        fun fromDb(db: Float): NoiseLevel = when {
            db < 40f -> QUIET
            db < 55f -> MODERATELY_QUIET
            db < 65f -> MODERATE
            db < 80f -> LOUD
            else -> VERY_LOUD
        }
    }
}

/**
 * Suitability score state
 */
enum class SuitabilityState(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    MODERATE("Moderate"),
    POOR("Poor"),
    NOT_RECOMMENDED("Not Recommended");

    companion object {
        fun fromScore(score: Int): SuitabilityState = when {
            score >= 90 -> EXCELLENT
            score >= 75 -> GOOD
            score >= 50 -> MODERATE
            score >= 25 -> POOR
            else -> NOT_RECOMMENDED
        }
    }
}

/**
 * Recommendation type for the recommendation card
 */
enum class RecommendationType {
    RECOMMENDED,
    CONSIDER_QUIETER,
    NOT_IDEAL
}
