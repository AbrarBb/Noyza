package com.khatibstudio.noyza.domain.engine

import com.khatibstudio.noyza.domain.model.ActivityType
import com.khatibstudio.noyza.domain.model.RecommendationType
import com.khatibstudio.noyza.domain.model.SuitabilityState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SuitabilityEngineTest {

    private lateinit var engine: SuitabilityEngine

    @Before
    fun setup() {
        engine = SuitabilityEngine()
    }

    @Test
    fun `quiet room for deep work produces high suitability score`() {
        val result = engine.calculate(
            activity = ActivityType.DEEP_WORK,
            averageDb = 38f,
            peakDb = 45f,
            minimumDb = 32f,
            stabilityPercent = 95f,
            loudTimePercent = 0f,
            samples = listOf(35f, 38f, 40f, 36f, 39f)
        )

        assertTrue("Expected score >= 80 but was ${result.score}", result.score >= 80)
        assertEquals(RecommendationType.RECOMMENDED, result.recommendation)
        assertEquals(SuitabilityState.EXCELLENT, result.state)
    }

    @Test
    fun `noisy environment for sleep produces not ideal score`() {
        val result = engine.calculate(
            activity = ActivityType.SLEEP,
            averageDb = 75f,
            peakDb = 85f,
            minimumDb = 65f,
            stabilityPercent = 40f,
            loudTimePercent = 80f,
            samples = listOf(70f, 75f, 85f, 80f, 72f)
        )

        assertTrue("Expected score < 50 but was ${result.score}", result.score < 50)
        assertEquals(RecommendationType.NOT_IDEAL, result.recommendation)
    }

    @Test
    fun `score is always bounded between 0 and 100`() {
        // Extreme low
        val extremeLow = engine.calculate(
            activity = ActivityType.RELAX,
            averageDb = 120f,
            peakDb = 130f,
            minimumDb = 110f,
            stabilityPercent = 0f,
            loudTimePercent = 100f
        )
        assertTrue(extremeLow.score in 0..100)

        // Extreme high
        val extremeHigh = engine.calculate(
            activity = ActivityType.STUDY,
            averageDb = 35f,
            peakDb = 40f,
            minimumDb = 30f,
            stabilityPercent = 100f,
            loudTimePercent = 0f
        )
        assertTrue(extremeHigh.score in 0..100)
    }

    @Test
    fun `distribution percentages sum to 100 percent`() {
        val samples = listOf(30f, 45f, 65f, 80f, 90f)
        val result = engine.calculate(
            activity = ActivityType.STUDY,
            averageDb = 55f,
            peakDb = 90f,
            minimumDb = 30f,
            stabilityPercent = 70f,
            loudTimePercent = 40f,
            samples = samples
        )

        val total = result.quietPercent + result.moderatePercent + result.loudPercent + result.veryLoudPercent
        assertEquals(100f, total, 0.1f)
    }

    @Test
    fun `moderate noise for casual reading produces acceptable or consider state`() {
        val result = engine.calculate(
            activity = ActivityType.READING,
            averageDb = 55f,
            peakDb = 62f,
            minimumDb = 48f,
            stabilityPercent = 80f,
            loudTimePercent = 5f
        )

        assertTrue("Expected score between 50 and 90 but was ${result.score}", result.score in 50..90)
    }
}
