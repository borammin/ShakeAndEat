package com.shakeandeat.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sqrt

/**
 * Decides when a stream of accelerometer readings counts as a shake. Plain Kotlin with the clock
 * passed in, so it can be unit tested without a phone.
 *
 * A shake is [REQUIRED_JOLTS] strong jolts (more than [THRESHOLD_G] times gravity) within
 * [WINDOW_MS]. After one fires, further shakes are ignored for [COOLDOWN_MS].
 */
class ShakeTrigger(
    private val thresholdG: Float = THRESHOLD_G,
    private val requiredJolts: Int = REQUIRED_JOLTS,
    private val windowMs: Long = WINDOW_MS,
    private val cooldownMs: Long = COOLDOWN_MS,
) {
    private var jolts = 0
    private var firstJoltAt = 0L
    private var lastJoltAt = 0L
    private var cooldownUntil = 0L

    /** Feeds one reading (m/s², including gravity). Returns true when a shake just happened. */
    fun onSample(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        if (nowMs < cooldownUntil) return false
        val g = sqrt(x * x + y * y + z * z) / GRAVITY
        if (g < thresholdG) return false
        // One physical jolt produces several consecutive readings, so count it once.
        if (jolts > 0 && nowMs - lastJoltAt < JOLT_GAP_MS) {
            lastJoltAt = nowMs
            return false
        }
        if (jolts == 0 || nowMs - firstJoltAt > windowMs) {
            jolts = 0
            firstJoltAt = nowMs
        }
        jolts++
        lastJoltAt = nowMs
        if (jolts >= requiredJolts) {
            jolts = 0
            cooldownUntil = nowMs + cooldownMs
            return true
        }
        return false
    }

    companion object {
        const val GRAVITY = 9.80665f
        const val THRESHOLD_G = 2.5f
        const val REQUIRED_JOLTS = 2
        const val WINDOW_MS = 1_000L
        const val COOLDOWN_MS = 1_500L
        const val JOLT_GAP_MS = 150L
    }
}

/** True if this device has an accelerometer. Without one, shake is simply never offered. */
fun hasAccelerometer(context: Context): Boolean {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    return manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/** Remembers [hasAccelerometer], so screens can show a "shake" hint only when it can work. */
@Composable
fun rememberShakeAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) { hasAccelerometer(context) }
}

/**
 * Calls [onShake] when the phone is shaken. It listens only while the screen is visible (resumed)
 * and [enabled] is true, and does nothing if the phone has no accelerometer. No permission is
 * needed for this sensor. Whatever [onShake] does must also be reachable by a button, because the
 * shake is an extra, not the only way.
 */
@Composable
fun ShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val latestOnShake by rememberUpdatedState(onShake)

    DisposableEffect(enabled, lifecycleOwner) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || manager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        val trigger = ShakeTrigger()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val v = event.values
                if (trigger.onSample(v[0], v[1], v[2], SystemClock.elapsedRealtime())) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    latestOnShake()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
                Lifecycle.Event.ON_PAUSE -> manager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.unregisterListener(listener)
        }
    }
}
