package com.videonatics.smartcapture.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class LevelSensorHelper(
    private val context: Context
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val _orientation =
        MutableStateFlow(Orientation(0f, 0f, false))
    val orientation: StateFlow<Orientation> = _orientation

    private var isRunning = false

    // smoothing
    private var smoothPitch = 0f
    private var smoothRoll = 0f

    // stability detection
    private var lastPitch = 0f
    private var lastRoll = 0f
    private var stableCounter = 0

    fun start() {
        if (sensor == null || isRunning) return
        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    fun reset() {
        stableCounter = 0
        _orientation.value = Orientation(0f, 0f, false)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR
        ) return

        val rotationMatrix = FloatArray(9)
        val adjustedMatrix = FloatArray(9)

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            axisX,
            axisY,
            adjustedMatrix
        )

        val orientationVals = FloatArray(3)
        SensorManager.getOrientation(adjustedMatrix, orientationVals)

        var pitch = Math.toDegrees(orientationVals[1].toDouble()).toFloat()
        var roll = Math.toDegrees(orientationVals[2].toDouble()).toFloat()

        // normalize
        if (roll > 90f) roll = 180f - roll
        if (roll < -90f) roll = -180f - roll
        pitch = -pitch

        val stable = detectStability(pitch, roll)

        // ✅ Emit absolute values (relative to gravity), don't subtract a base "calibration" value
        // if you want the level to show actual tilt relative to the earth.
        smoothPitch = smooth(pitch, smoothPitch)
        smoothRoll = smooth(roll, smoothRoll)

        _orientation.value = Orientation(
            pitch = smoothPitch,
            roll = smoothRoll,
            isCalibrated = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectStability(pitch: Float, roll: Float): Boolean {
        val delta =
            kotlin.math.abs(pitch - lastPitch) +
                    kotlin.math.abs(roll - lastRoll)

        lastPitch = pitch
        lastRoll = roll

        return if (delta < 0.2f) {
            stableCounter++
            stableCounter > 8
        } else {
            stableCounter = 0
            false
        }
    }

    private fun smooth(new: Float, prev: Float): Float {
        val delta = kotlin.math.abs(new - prev)
        val alpha = when {
            delta > 10f -> 0.25f
            delta > 3f -> 0.15f
            else -> 0.05f
        }
        return prev + alpha * (new - prev)
    }
}

data class Orientation(
    val pitch: Float,
    val roll: Float,
    val isCalibrated: Boolean
)
