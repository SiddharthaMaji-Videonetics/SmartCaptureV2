package com.videonatics.smartcapture.feature.capture.presentation.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun HorizonLevelOverlay(
    pitch: Float,
    roll: Float,
    onLevel:(Boolean)-> Unit ={},
    modifier: Modifier = Modifier
) {
    val maxAngle = 45f
    val deadZone = 2f

    val correctedPitch = if (abs(pitch) < deadZone) 0f else pitch
    val correctedRoll = if (abs(roll) < deadZone) 0f else roll

    val targetPitch = (correctedPitch.coerceIn(-maxAngle, maxAngle)) / maxAngle
    val targetRoll = (correctedRoll.coerceIn(-maxAngle, maxAngle)) / maxAngle

    val animatedPitch by animateFloatAsState(targetPitch, label = "pitch_anim")
    val animatedRoll by animateFloatAsState(targetRoll, label = "roll_anim")

    val isLevel = abs(pitch) < deadZone && abs(roll) < deadZone

    onLevel.invoke(isLevel)
    // Instruction logic

    val instruction = remember(pitch, roll) {
        when {
            isLevel -> {
                "Perfectly level"
            }

            abs(roll) > abs(pitch) -> {
                if (roll > 0) "Tilt left" else "Tilt right"
            }

            else -> {
                if (pitch > 0) "Move down" else "Move up"
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.matchParentSize()) {

            val center = center

            val rawOffset = animatedPitch * size.height / 6f
            val maxOffset = size.height / 4f
            val verticalOffset = rawOffset.coerceIn(-maxOffset, maxOffset)

            val rotationDegrees = animatedRoll * 45f

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

            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 3.dp.toPx(),
                center = center
            )
        }

        //Bottom Instruction
        AnimatedVisibility(
            visible = !isLevel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = instruction,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Optional: show "Level" feedback
        AnimatedVisibility(
            visible = isLevel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Level ✓",
                color = Color(0xFF7787FF),
                fontSize = 14.sp
            )
        }
    }
}