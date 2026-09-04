package com.khatibstudio.noyza.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FftAudioProcessorTest {

    private lateinit var processor: FftAudioProcessor

    @Before
    fun setup() {
        processor = FftAudioProcessor(sampleRate = 44100, fftSize = 1024)
    }

    private fun generateSineWavePcm(freqHz: Double, sampleRate: Int = 44100, numSamples: Int = 1024): ShortArray {
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * freqHz * i / sampleRate
            buffer[i] = (sin(angle) * 16000.0).toInt().toShort()
        }
        return buffer
    }

    @Test
    fun `silent buffer produces zero energy without NaN or exception`() {
        val silentBuffer = ShortArray(1024) { 0 }
        val profile = processor.process(silentBuffer, 1024)

        assertFalse(profile.lowPercent.isNaN())
        assertFalse(profile.midPercent.isNaN())
        assertFalse(profile.highPercent.isNaN())
        assertEquals(SoundCharacter.BALANCED, profile.character)
    }

    @Test
    fun `low frequency 100Hz tone identifies as LOW_RUMBLE`() {
        val lowPcm = generateSineWavePcm(freqHz = 100.0)
        val profile = processor.process(lowPcm, 1024)

        assertTrue("Expected lowPercent > 50f but got ${profile.lowPercent}", profile.lowPercent > 50f)
        assertEquals(SoundCharacter.LOW_RUMBLE, profile.character)
    }

    @Test
    fun `mid frequency 1000Hz speech-range tone identifies as SPEECH_HEAVY`() {
        val midPcm = generateSineWavePcm(freqHz = 1000.0)
        val profile = processor.process(midPcm, 1024)

        assertTrue("Expected midPercent > 50f but got ${profile.midPercent}", profile.midPercent > 50f)
        assertEquals(SoundCharacter.SPEECH_HEAVY, profile.character)
    }

    @Test
    fun `high frequency 6000Hz tone identifies as SHARP_CLATTER`() {
        val highPcm = generateSineWavePcm(freqHz = 6000.0)
        val profile = processor.process(highPcm, 1024)

        assertTrue("Expected highPercent > 35f but got ${profile.highPercent}", profile.highPercent > 35f)
        assertEquals(SoundCharacter.SHARP_CLATTER, profile.character)
    }
}
