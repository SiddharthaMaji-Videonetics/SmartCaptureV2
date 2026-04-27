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

    private val _orientation = MutableStateFlow(Orientation(0f, 0f))
    val orientation: StateFlow<Orientation> = _orientation

    private var isRunning = false

    // smoothing
    private var smoothPitch = 0f
    private var smoothRoll = 0f

    // calibration
    private var basePitch: Float? = null
    private var baseRoll: Float? = null

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

    fun recalibrate() {
        basePitch = null
        baseRoll = null
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR
        ) return

        val rotationMatrix = FloatArray(9)
        val adjustedMatrix = FloatArray(9)

        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            event.values
        )

        // ✅ Screen rotation compensation
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

        // 🔥 Gravity vector (Z axis of device)
        val gX = adjustedMatrix[6]
        val gY = adjustedMatrix[7]
        val gZ = adjustedMatrix[8]

        // ✅ Decoupled pitch (forward/back tilt)
        val rawPitch = Math.toDegrees(
            atan2(
                -gY.toDouble(),
                sqrt((gX * gX + gZ * gZ).toDouble())
            )
        ).toFloat()

        // ✅ Decoupled roll (left/right tilt) — FIXED
        val rawRoll = Math.toDegrees(
            atan2(
                gX.toDouble(),
                sqrt((gY * gY + gZ * gZ).toDouble())
            )
        ).toFloat()

        // ✅ Calibration (removes bias)
        if (basePitch == null) basePitch = rawPitch
        if (baseRoll == null) baseRoll = rawRoll

        val calibratedPitch = rawPitch - (basePitch ?: 0f)
        val calibratedRoll = rawRoll - (baseRoll ?: 0f)

        // ✅ Smoothing
        smoothPitch = smooth(calibratedPitch, smoothPitch)
        smoothRoll = smooth(calibratedRoll, smoothRoll)

        _orientation.value = Orientation(
            pitch = smoothPitch,
            roll = smoothRoll
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun smooth(new: Float, prev: Float, alpha: Float = 0.1f): Float {
        return prev + alpha * (new - prev)
    }
}

data class Orientation(
    val pitch: Float,
    val roll: Float
)