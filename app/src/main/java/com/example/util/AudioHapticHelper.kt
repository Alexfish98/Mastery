package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioHapticHelper(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playSound(isCorrect: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            val freq = if (isCorrect) 660.0 else 220.0
            val durationSec = if (isCorrect) 0.12 else 0.22
            playSineWave(freq, durationSec)
        }
    }

    fun playChime() {
        CoroutineScope(Dispatchers.Default).launch {
            playSineWave(523.25, 0.1) // C5
            kotlinx.coroutines.delay(100)
            playSineWave(659.25, 0.15) // E5
            kotlinx.coroutines.delay(120)
            playSineWave(783.99, 0.25) // G5
        }
    }

    fun playFeedback(isCorrect: Boolean) {
        playSound(isCorrect)
    }

    fun vibrateAlert() {
        vibrate(false)
    }

    fun vibrate(isCorrect: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isCorrect) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    val timings = longArrayOf(0, 70, 50, 70)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isCorrect) 40L else 120L)
            }
        } catch (_: Exception) {}
    }

    private fun playSineWave(freq: Double, durationSec: Double) {
        try {
            val sampleRate = 44100
            val numSamples = (durationSec * sampleRate).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                // Fade out envelope at the end to prevent clicking
                val envelope = if (i > numSamples - 500) (numSamples - i).toDouble() / 500.0 else 1.0
                buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.25 * envelope).toInt().toShort()
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((durationSec * 1000).toLong() + 50)
            track.release()
        } catch (_: Exception) {}
    }
}
