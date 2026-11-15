package com.rynekryz.celestite.core.designsystem

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private var cachedVibrator: Vibrator? = null
private var vibratorFetched = false

private fun Context.vibratorOrNull(): Vibrator? {
    if (vibratorFetched) return cachedVibrator
    vibratorFetched = true
    cachedVibrator = runCatching {
        (applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    }.getOrNull()
    return cachedVibrator
}

private fun Vibrator.oneShot(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
    if (!hasVibrator()) return
    runCatching {
        vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
    }
}

private fun Vibrator.pattern(timings: LongArray, amplitudes: IntArray) {
    if (!hasVibrator()) return
    runCatching {
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
}

class Haptics(
    private val vibrator: Vibrator?,
) {
    fun tick() {
        vibrator?.oneShot(20, 90)
    }

    fun click() {
        vibrator?.oneShot(25, 150)
    }

    fun confirm() {
        vibrator?.pattern(longArrayOf(0, 12, 40, 18), intArrayOf(0, 140, 0, 200))
    }

    fun reject() {
        vibrator?.pattern(longArrayOf(0, 20, 60, 20, 60, 20), intArrayOf(0, 180, 0, 180, 0, 180))
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember { Haptics(context.vibratorOrNull()) }
}
