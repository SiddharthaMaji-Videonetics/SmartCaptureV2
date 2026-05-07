package com.videonatics.smartcapture.feature.capturelist.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.parseOutput
import com.videonatics.smartcapture.core.preprocessBitmap
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.toRect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.tensorflow.lite.Interpreter
import javax.inject.Inject
import kotlin.math.PI

@HiltViewModel
class ImageEditorViewModel @Inject constructor() : ViewModel() {

    private val _detectedBox = MutableStateFlow<RectF?>(null)
    val detectedBox: Flow<RectF?> = _detectedBox

    private val _detectedBoxAngle = MutableStateFlow<Float?>(null)
    val detectedBoxAngle: Flow<Float?> = _detectedBoxAngle

    fun precessImage(
        interpreter: Interpreter?,
        image: Bitmap
    ) {
        val prep = preprocessBitmap(bitmap = image, 416)
        val output = Array(1) { Array(6) { FloatArray(3549) } }
        interpreter?.run(prep.buffer, output)
        val finalDetections = parseOutput(
            output,
            prep.ratio,
            prep.dx,
            prep.dy,
            confThreshold = 0.8f,
            iouThreshold = 0.35f,
            imgSize = 416
        )
        val detection = finalDetections.firstOrNull()
        val imageWidth = prep.srcWidth.toFloat()
        val imageHeight = prep.srcHeight.toFloat()

        if (detection != null) {
            _detectedBox.value = detection.toRect().toNormalized(imageWidth, imageHeight)
            _detectedBoxAngle.value = (detection.angle * 180.0 / PI).toFloat()
        } else {
            _detectedBox.value = null
            _detectedBoxAngle.value = null
        }
    }
}