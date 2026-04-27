package com.videonatics.smartcapture.feature.capture.presentation.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun HorizonLevelOverlay(
    pitch: Float,
    roll: Float,
    modifier: Modifier = Modifier
) {
    val maxAngle = 45f
    val deadZone = 2f

    //Snap near level
    val correctedPitch = if (abs(pitch) < deadZone) 0f else pitch
    val correctedRoll = if (abs(roll) < deadZone) 0f else roll

    // Normalize
    val targetPitch = (correctedPitch.coerceIn(-maxAngle, maxAngle)) / maxAngle
    val targetRoll = (correctedRoll.coerceIn(-maxAngle, maxAngle)) / maxAngle

    val animatedPitch by animateFloatAsState(targetPitch, label = "pitch_anim")
    val animatedRoll by animateFloatAsState(targetRoll, label = "roll_anim")

    Canvas(modifier = modifier.fillMaxSize()) {

        val center = center

        // Less aggressive movement
        val rawOffset = animatedPitch * size.height / 6f
        val maxOffset = size.height / 4f
        val verticalOffset = rawOffset.coerceIn(-maxOffset, maxOffset)

        val rotationDegrees = animatedRoll * 45f

        val isLevel = abs(pitch) < 2 && abs(roll) < 2

        rotate(rotationDegrees, pivot = center) {

            val y = center.y + verticalOffset

            drawLine(
                color = if (isLevel) Color(0xFF7787FF) else Color(0xFF676A85),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (isLevel) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Center indicator (important visual reference)
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 3.dp.toPx(),
            center = center
        )
    }
}