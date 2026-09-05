package com.khatibstudio.noyza.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Characterization of the acoustic environment based on spectral distribution.
 */
enum class SoundCharacter(val displayName: String, val description: String) {
    BALANCED("Balanced Ambient", "Evenly distributed sound spectrum"),
    SPEECH_HEAVY("Speech & Voices", "Dominated by human vocal frequencies (250–4000 Hz)"),
    LOW_RUMBLE("HVAC & Low Hum", "Low-frequency rumble from ventilation, engines, or traffic"),
    SHARP_CLATTER("Sharp Clatter", "High-frequency transients such as dishes, clicks, or typing")
}

/**
 * Real-time spectral snapshot computed from PCM audio.
 */
data class SoundProfile(
    val lowPercent: Float = 33.3f,      // 20–250 Hz
    val midPercent: Float = 33.3f,      // 250–4000 Hz
    val highPercent: Float = 33.4f,     // 4000–12000 Hz
    val character: SoundCharacter = SoundCharacter.BALANCED,
    val peakFrequencyHz: Float = 0f
)

/**
 * Fast Fourier Transform (FFT) processor for 16-bit PCM audio samples.
 *
 * Provides real-time frequency-aware analysis to distinguish low HVAC hum,
 * vocal chatter, and sharp clatter, enabling human-perceived psychoacoustic weighting.
 */
class FftAudioProcessor(
    private val sampleRate: Int = 44100,
    private val fftSize: Int = 1024
) {
    init {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) {
            "FFT size must be a power of 2"
        }
    }

    private val hanningWindow = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }

    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)

    // Bins correspond to frequency ranges
    // binFrequency = binIndex * (sampleRate / fftSize) = binIndex * ~43.06 Hz
    private val binWidth = sampleRate.toFloat() / fftSize
    private val lowCutoffBin = max(1, (250f / binWidth).toInt())       // ~6
    private val midCutoffBin = minOf(fftSize / 2, (4000f / binWidth).toInt())  // ~93
    private val highCutoffBin = minOf(fftSize / 2, (12000f / binWidth).toInt()) // ~278

    // EMA smoothing state for stable spectral distribution
    private var hasPreviousSample = false
    private var smoothedLow = 33.3f
    private var smoothedMid = 33.3f
    private var smoothedHigh = 33.4f
    private var currentCharacter = SoundCharacter.BALANCED
    private val SPECTRAL_ALPHA = 0.15f

    fun reset() {
        hasPreviousSample = false
        smoothedLow = 33.3f
        smoothedMid = 33.3f
        smoothedHigh = 33.4f
        currentCharacter = SoundCharacter.BALANCED
    }

    /**
     * Process a chunk of PCM audio samples and compute the frequency profile.
     */
    fun process(buffer: ShortArray, readCount: Int): SoundProfile {
        if (readCount < fftSize) {
            return SoundProfile(
                lowPercent = smoothedLow,
                midPercent = smoothedMid,
                highPercent = smoothedHigh,
                character = currentCharacter
            )
        }

        // Windowing and loading into real buffer
        for (i in 0 until fftSize) {
            real[i] = (buffer[i] / 32768.0f) * hanningWindow[i]
            imag[i] = 0f
        }

        // Perform in-place Radix-2 Cooley-Tukey FFT
        computeFft(real, imag, fftSize)

        // Calculate power in each frequency band
        var lowEnergy = 0f
        var midEnergy = 0f
        var highEnergy = 0f
        var maxMagnitude = 0f
        var peakBin = 0

        val halfSize = fftSize / 2
        for (i in 1 until halfSize) {
            val magnitude = sqrt(real[i] * real[i] + imag[i] * imag[i])

            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                peakBin = i
            }

            when {
                i < lowCutoffBin -> lowEnergy += magnitude
                i < midCutoffBin -> midEnergy += magnitude
                i < highCutoffBin -> highEnergy += magnitude
            }
        }

        val totalEnergy = lowEnergy + midEnergy + highEnergy
        if (totalEnergy <= 0.0001f) {
            return SoundProfile(
                lowPercent = smoothedLow,
                midPercent = smoothedMid,
                highPercent = smoothedHigh,
                character = currentCharacter
            )
        }

        val rawLow = (lowEnergy / totalEnergy * 100f).coerceIn(0f, 100f)
        val rawMid = (midEnergy / totalEnergy * 100f).coerceIn(0f, 100f)
        val rawHigh = (highEnergy / totalEnergy * 100f).coerceIn(0f, 100f)

        if (!hasPreviousSample) {
            smoothedLow = rawLow
            smoothedMid = rawMid
            smoothedHigh = rawHigh
            hasPreviousSample = true
            currentCharacter = evaluateCharacter(rawLow, rawMid, rawHigh)
        } else {
            // Exponential Moving Average to prevent rapid frame-by-frame flutter
            smoothedLow = (SPECTRAL_ALPHA * rawLow) + ((1f - SPECTRAL_ALPHA) * smoothedLow)
            smoothedMid = (SPECTRAL_ALPHA * rawMid) + ((1f - SPECTRAL_ALPHA) * smoothedMid)
            smoothedHigh = (SPECTRAL_ALPHA * rawHigh) + ((1f - SPECTRAL_ALPHA) * smoothedHigh)

            // Hysteresis thresholding for sound character to eliminate border flip-flopping
            currentCharacter = when (currentCharacter) {
                SoundCharacter.SPEECH_HEAVY -> {
                    if (smoothedMid < 45f) evaluateCharacter(smoothedLow, smoothedMid, smoothedHigh)
                    else SoundCharacter.SPEECH_HEAVY
                }
                SoundCharacter.LOW_RUMBLE -> {
                    if (smoothedLow < 45f) evaluateCharacter(smoothedLow, smoothedMid, smoothedHigh)
                    else SoundCharacter.LOW_RUMBLE
                }
                SoundCharacter.SHARP_CLATTER -> {
                    if (smoothedHigh < 30f) evaluateCharacter(smoothedLow, smoothedMid, smoothedHigh)
                    else SoundCharacter.SHARP_CLATTER
                }
                SoundCharacter.BALANCED -> {
                    evaluateCharacter(smoothedLow, smoothedMid, smoothedHigh)
                }
            }
        }

        val peakFreq = peakBin * binWidth

        return SoundProfile(
            lowPercent = smoothedLow,
            midPercent = smoothedMid,
            highPercent = smoothedHigh,
            character = currentCharacter,
            peakFrequencyHz = peakFreq
        )
    }

    private fun evaluateCharacter(low: Float, mid: Float, high: Float): SoundCharacter = when {
        mid >= 50f -> SoundCharacter.SPEECH_HEAVY
        low >= 50f -> SoundCharacter.LOW_RUMBLE
        high >= 35f -> SoundCharacter.SHARP_CLATTER
        else -> SoundCharacter.BALANCED
    }

    /**
     * In-place Radix-2 Cooley-Tukey FFT.
     */
    private fun computeFft(re: FloatArray, im: FloatArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempRe = re[i]
                re[i] = re[j]
                re[j] = tempRe

                val tempIm = im[i]
                im[i] = im[j]
                im[j] = tempIm
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey butterflies
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepRe = cos(angle).toFloat()
            val wStepIm = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wRe = 1.0f
                var wIm = 0.0f
                for (k in 0 until halfLen) {
                    val pos = i + k
                    val partner = pos + halfLen

                    val tRe = wRe * re[partner] - wIm * im[partner]
                    val tIm = wRe * im[partner] + wIm * re[partner]

                    val uRe = re[pos]
                    val uIm = im[pos]

                    re[pos] = uRe + tRe
                    im[pos] = uIm + tIm
                    re[partner] = uRe - tRe
                    im[partner] = uIm - tIm

                    val nextWRe = wRe * wStepRe - wIm * wStepIm
                    val nextWIm = wRe * wStepIm + wIm * wStepRe
                    wRe = nextWRe
                    wIm = nextWIm
                }
                i += len
            }
            len = len shl 1
        }
    }
}
