package com.khatibstudio.noyza.domain.engine

import com.khatibstudio.noyza.audio.SoundCharacter
import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.CustomActivityProfile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SuitabilityEnginePsychoacousticTest {

    private lateinit var engine: SuitabilityEngine

    @Before
    fun setup() {
        engine = SuitabilityEngine()
    }

    @Test
    fun `speech heavy sound character produces lower score than balanced for focus activities`() {
        val balancedResult = engine.calculate(
            activity = ActivityType.DEEP_WORK,
            averageDb = 50f,
            peakDb = 58f,
            minimumDb = 45f,
            stabilityPercent = 85f,
            loudTimePercent = 5f,
            soundCharacter = SoundCharacter.BALANCED
        )

        val speechResult = engine.calculate(
            activity = ActivityType.DEEP_WORK,
            averageDb = 50f,
            peakDb = 58f,
            minimumDb = 45f,
            stabilityPercent = 85f,
            loudTimePercent = 5f,
            soundCharacter = SoundCharacter.SPEECH_HEAVY
        )

        assertTrue(
            "Expected speech score (${speechResult.score}) to be strictly less than balanced score (${balancedResult.score})",
            speechResult.score < balancedResult.score
        )
    }

    @Test
    fun `low rumble provides masking relief compared to speech heavy for reading`() {
        val speechResult = engine.calculate(
            activity = ActivityType.READING,
            averageDb = 48f,
            peakDb = 55f,
            minimumDb = 42f,
            stabilityPercent = 90f,
            loudTimePercent = 0f,
            soundCharacter = SoundCharacter.SPEECH_HEAVY
        )

        val rumbleResult = engine.calculate(
            activity = ActivityType.READING,
            averageDb = 48f,
            peakDb = 55f,
            minimumDb = 42f,
            stabilityPercent = 90f,
            loudTimePercent = 0f,
            soundCharacter = SoundCharacter.LOW_RUMBLE
        )

        assertTrue(
            "Expected low rumble score (${rumbleResult.score}) to be higher than speech score (${speechResult.score})",
            rumbleResult.score > speechResult.score
        )
    }

    @Test
    fun `custom activity profile evaluates suitability accurately based on custom thresholds`() {
        val customProfile = CustomActivityProfile(
            id = 1L,
            displayName = "Podcast Studio",
            iconName = "Mic",
            idealMinDb = 25f,
            idealMaxDb = 40f,
            acceptableMaxDb = 48f,
            spikeSensitivity = 0.9f
        )

        val quietStudio = engine.calculate(
            activity = customProfile,
            averageDb = 32f,
            peakDb = 38f,
            minimumDb = 28f,
            stabilityPercent = 98f,
            loudTimePercent = 0f
        )

        val noisyStudio = engine.calculate(
            activity = customProfile,
            averageDb = 56f,
            peakDb = 68f,
            minimumDb = 45f,
            stabilityPercent = 60f,
            loudTimePercent = 40f
        )

        assertTrue("Quiet studio score should be high (>= 85) but got ${quietStudio.score}", quietStudio.score >= 85)
        assertTrue("Noisy studio score should be low (< 60) but got ${noisyStudio.score}", noisyStudio.score < 60)
    }
}
