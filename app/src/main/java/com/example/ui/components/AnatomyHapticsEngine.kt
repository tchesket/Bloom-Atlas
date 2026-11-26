package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.SensitivityTier

/**
 * Translates biological nerve-ending density across anatomical zones into
 * nuanced tactile vibration patterns (gentle micro-flutter on the clitoral glans,
 * warm double-ripple on high-sensitivity erectile/mucosal folds, subtle grounding
 * tap on supportive/structural regions).
 */
class AnatomyHapticsEngine(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private var lastDragPulseMs: Long = 0L

    fun triggerTap(tier: SensitivityTier) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (tier) {
                    SensitivityTier.ULTRA -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 26L, 38L, 36L, 40L, 52L),
                        intArrayOf(0, 120, 0, 185, 0, 245),
                        -1
                    )
                    SensitivityTier.HIGH -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 26L, 44L, 38L),
                        intArrayOf(0, 115, 0, 175),
                        -1
                    )
                    SensitivityTier.MODERATE -> VibrationEffect.createOneShot(24L, 95)
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (tier) {
                    SensitivityTier.ULTRA -> v.vibrate(longArrayOf(0L, 26L, 38L, 36L, 40L, 52L), -1)
                    SensitivityTier.HIGH -> v.vibrate(longArrayOf(0L, 26L, 44L, 38L), -1)
                    SensitivityTier.MODERATE -> v.vibrate(24L)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore if device/emulator restricts vibrator
        }
    }

    fun triggerDragPulse(tier: SensitivityTier) {
        val now = System.currentTimeMillis()
        val cooldownMs = when (tier) {
            SensitivityTier.ULTRA -> 140L
            SensitivityTier.HIGH -> 190L
            SensitivityTier.MODERATE -> 260L
        }
        if (now - lastDragPulseMs < cooldownMs) return
        lastDragPulseMs = now

        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (tier) {
                    SensitivityTier.ULTRA -> VibrationEffect.createWaveform(
                        longArrayOf(0L, 18L, 26L, 24L),
                        intArrayOf(0, 145, 0, 210),
                        -1
                    )
                    SensitivityTier.HIGH -> VibrationEffect.createOneShot(20L, 135)
                    SensitivityTier.MODERATE -> VibrationEffect.createOneShot(14L, 75)
                }
                v.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(if (tier == SensitivityTier.ULTRA) 24L else 16L)
            }
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }

    /**
     * Plays a smooth continuous vibration slice whose intensity ramps cleanly from low (`0f`)
     * to maximum (`1f`). Uses hardware amplitude control when available, falling back to a
     * smooth high-rate duty-cycle waveform on vibrators without amplitude control.
     */
    fun playContinuousIntensity(intensity01: Float, durationMs: Long = 240L) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val clamped = intensity01.coerceIn(0f, 1f)
        val safeDur = durationMs.coerceIn(60L, 1_000L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (v.hasAmplitudeControl()) {
                    // Perceptually smooth climb from gentle rumble (28/255) up to full continuous motor output (255/255)
                    val shaped = clamped * (0.42f + 0.58f * clamped)
                    val amp = (28f + 227f * shaped).toInt().coerceIn(28, 255)
                    v.vibrate(VibrationEffect.createOneShot(safeDur, amp))
                } else if (clamped >= 0.85f) {
                    v.vibrate(VibrationEffect.createOneShot(safeDur, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Duty-cycle intensity ramp for fixed-amplitude vibrators
                    val periodMs = 40L
                    val onMs = (12L + (26f * clamped).toLong()).coerceIn(12L, 38L)
                    val offMs = (periodMs - onMs).coerceAtLeast(2L)
                    val cycles = (safeDur / periodMs).toInt().coerceIn(2, 12)
                    val timings = LongArray(cycles * 2)
                    val amps = IntArray(cycles * 2)
                    for (i in 0 until cycles) {
                        timings[i * 2] = onMs
                        amps[i * 2] = VibrationEffect.DEFAULT_AMPLITUDE
                        timings[i * 2 + 1] = offMs
                        amps[i * 2 + 1] = 0
                    }
                    v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(safeDur)
            }
        } catch (_: Exception) {
            // Gracefully ignore if device restricts vibrator
        }
    }

    /**
     * Plays a high-intensity, high-frequency, slightly irregular pulsing vibration waveform
     * chunk during the 12-second peak episode.
     */
    fun playIrregularPeakWaveform(timings: LongArray, amplitudes: IntArray) {
        if (timings.isEmpty() || timings.size != amplitudes.size) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val safeAmps = if (v.hasAmplitudeControl()) {
                    IntArray(amplitudes.size) { i ->
                        val a = amplitudes[i]
                        if (a <= 0) 0 else a.coerceIn(1, 255)
                    }
                } else {
                    IntArray(amplitudes.size) { i ->
                        if (amplitudes[i] <= 0) 0 else VibrationEffect.DEFAULT_AMPLITUDE
                    }
                }
                v.vibrate(VibrationEffect.createWaveform(timings, safeAmps, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }

    /** Immediately stops any active continuous or pulsing vibration. */
    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }
}
