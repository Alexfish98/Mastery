package com.example.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.Random
import kotlin.math.sin

/**
 * Generates synthetic, 100% offline ambient soundscapes using raw PCM audio and AudioTrack.
 * No external files or network calls required.
 */
class AmbientAudioGenerator {

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private var currentType: AmbientType? = null

    enum class AmbientType(val displayName: String, val description: String) {
        ALPHA_WAVES("Alpha Focus Waves", "10Hz binaural beats for deep concentration"),
        RAIN("Calm Rainfall", "Gentle acoustic rain noise for study masking"),
        BROWN_NOISE("Deep Brown Noise", "Low-frequency rumble that calms racing thoughts"),
        FOREST_STREAM("Forest Stream", "Flowing water soundscapes with randomized ripples")
    }

    val isPlaying: Boolean
        get() = job?.isActive == true

    fun getCurrentType(): AmbientType? = currentType

    fun start(type: AmbientType) {
        stop()
        currentType = type

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        job = CoroutineScope(Dispatchers.Default).launch {
            val shortBuffer = ShortArray(4096)
            val random = Random()
            var phaseL = 0.0
            var phaseR = 0.0
            var brownL = 0.0
            var brownR = 0.0

            while (isActive) {
                when (type) {
                    AmbientType.ALPHA_WAVES -> {
                        // Left: 200 Hz, Right: 210 Hz (Delta = 10 Hz Alpha wave)
                        val freqL = 200.0
                        val freqR = 210.0
                        for (i in 0 until shortBuffer.size step 2) {
                            val sampleL = (sin(phaseL) * 9000).toInt().toShort()
                            val sampleR = (sin(phaseR) * 9000).toInt().toShort()
                            shortBuffer[i] = sampleL
                            shortBuffer[i + 1] = sampleR

                            phaseL += 2.0 * Math.PI * freqL / sampleRate
                            phaseR += 2.0 * Math.PI * freqR / sampleRate
                            if (phaseL > 2.0 * Math.PI) phaseL -= 2.0 * Math.PI
                            if (phaseR > 2.0 * Math.PI) phaseR -= 2.0 * Math.PI
                        }
                    }
                    AmbientType.RAIN -> {
                        for (i in 0 until shortBuffer.size step 2) {
                            val whiteL = (random.nextGaussian() * 3200).coerceIn(-18000.0, 18000.0)
                            val whiteR = (random.nextGaussian() * 3200).coerceIn(-18000.0, 18000.0)
                            brownL = (brownL * 0.96) + (whiteL * 0.04)
                            brownR = (brownR * 0.96) + (whiteR * 0.04)

                            // Rain drops burst
                            val dropL = if (random.nextFloat() < 0.003) (random.nextInt(6000) - 3000) else 0
                            val dropR = if (random.nextFloat() < 0.003) (random.nextInt(6000) - 3000) else 0

                            shortBuffer[i] = (brownL + dropL).toInt().coerceIn(-32767, 32767).toShort()
                            shortBuffer[i + 1] = (brownR + dropR).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                    AmbientType.BROWN_NOISE -> {
                        for (i in 0 until shortBuffer.size step 2) {
                            val whiteL = random.nextGaussian() * 2500
                            val whiteR = random.nextGaussian() * 2500
                            brownL = (brownL * 0.97) + (whiteL * 0.03)
                            brownR = (brownR * 0.97) + (whiteR * 0.03)
                            shortBuffer[i] = brownL.toInt().coerceIn(-32767, 32767).toShort()
                            shortBuffer[i + 1] = brownR.toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                    AmbientType.FOREST_STREAM -> {
                        for (i in 0 until shortBuffer.size step 2) {
                            val whiteL = random.nextGaussian() * 3500
                            val whiteR = random.nextGaussian() * 3500
                            brownL = (brownL * 0.93) + (whiteL * 0.07)
                            brownR = (brownR * 0.93) + (whiteR * 0.07)
                            shortBuffer[i] = brownL.toInt().coerceIn(-32767, 32767).toShort()
                            shortBuffer[i + 1] = brownR.toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                }

                val written = audioTrack?.write(shortBuffer, 0, shortBuffer.size) ?: -1
                if (written < 0) break
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        currentType = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
