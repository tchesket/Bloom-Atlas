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
}
