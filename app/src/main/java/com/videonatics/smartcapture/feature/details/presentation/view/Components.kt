package com.videonatics.smartcapture.feature.details.presentation.view

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.videonatics.smartcapture.core.domain.model.Detection

@Composable
fun ZoomableImageWithBoundingBox(
    bitmap: Bitmap,
    rect: RectF,
    imageWidth: Float,
    imageHeight: Float,
    modifier: Modifier = Modifier,
    boxColor: Color = Color.Red,
    strokeWidth: Dp = 3.dp
) {
    var userScale by remember { mutableStateOf(1f) }
    var userOffset by remember { mutableStateOf(Offset.Zero) }

    val minScale = 1f
    val maxScale = 5f

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (userScale * zoomChange).coerceIn(minScale, maxScale)

        // Adjust pan sensitivity based on zoom
        val scaleFactor = newScale / userScale
        userScale = newScale

        userOffset += panChange
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        userScale = 1f
                        userOffset = Offset.Zero
                    }
                )
            }
            .transformable(state = transformState)
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            if (imageWidth == 0f || imageHeight == 0f) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            // 🔹 Base FIT scale (like ImageView FIT_CENTER)
            val baseScale = minOf(
                canvasW / imageWidth,
                canvasH / imageHeight
            )

            val displayedW = imageWidth * baseScale
            val displayedH = imageHeight * baseScale

            // 🔹 TRUE CENTERING (this is the key)
            val centerOffset = Offset(
                (canvasW - displayedW) / 2f,
                (canvasH - displayedH) / 2f
            )

            val finalScale = baseScale * userScale

            // 🔥 IMPORTANT: Build final translation in screen space
            val finalOffset = centerOffset + userOffset

            withTransform({

                // Apply translation in screen space
                translate(finalOffset.x, finalOffset.y)

                // Then scale inside image space
                scale(finalScale, finalScale)

            }) {

                // Draw image in ORIGINAL coordinate system
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = IntSize(
                        imageWidth.toInt(),
                        imageHeight.toInt()
                    )
                )

                // Draw bounding box in SAME coordinate system
                drawRect(
                    color = boxColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width(), rect.height()),
                    style = Stroke(width = strokeWidth.toPx() / finalScale)
                )
            }
        }
    }
}