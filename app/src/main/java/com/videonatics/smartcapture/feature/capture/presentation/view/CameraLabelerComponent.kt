package com.videonatics.smartcapture.feature.capture.presentation.view

import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.videonatics.smartcapture.core.LevelSensorHelper
import kotlin.math.abs

@Composable
fun HorizonLevelOverlay(
    pitch: Float,
    roll: Float,
    onLevel: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val maxAngle = 45f
    val deadZone = 2f

    val isLevel = abs(pitch) < deadZone && abs(roll) < deadZone

    // normalize
    val targetPitch = (pitch.coerceIn(-maxAngle, maxAngle)) / maxAngle
    val targetRoll = (roll.coerceIn(-maxAngle, maxAngle)) / maxAngle

    val animatedPitch by animateFloatAsState(targetPitch, label = "pitch")
    val animatedRoll by animateFloatAsState(targetRoll, label = "roll")

    //avoid recomposition spam
    LaunchedEffect(isLevel) {
        onLevel(isLevel)
    }

    // instruction synced with UI (not raw values)
    val instruction = remember(animatedPitch, animatedRoll, isLevel) {
        val p = animatedPitch * maxAngle
        val r = animatedRoll * maxAngle

        when {
            isLevel -> "Perfectly level"
            abs(r) > abs(p) -> if (r > 0) "Tilt left" else "Tilt right"
            else -> if (p > 0) "Move down" else "Move up"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Canvas(modifier = Modifier.matchParentSize()) {

            val center = center

            val verticalOffset = (animatedPitch * size.height / 6f)
                .coerceIn(-size.height / 4f, size.height / 4f)

            val rotation = animatedRoll * 45f

            rotate(rotation, pivot = center) {
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

        // Instruction
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

        // Level indicator
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

@Composable
fun rememberLevelSensorHelper(): LevelSensorHelper {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val helper = remember { LevelSensorHelper(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    helper.start()

                    // delay → let sensor stabilize
                    Handler(Looper.getMainLooper()).postDelayed({
                        helper.recalibrate()
                    }, 300)
                }

                Lifecycle.Event.ON_PAUSE -> {
                    helper.stop()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            helper.stop()
        }
    }

    return helper
}