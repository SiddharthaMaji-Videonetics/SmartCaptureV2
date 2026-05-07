package com.videonatics.smartcapture.feature.capturelist.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.parseOutput
import com.videonatics.smartcapture.core.preprocess
import com.videonatics.smartcapture.core.preprocessBitmap
import com.videonatics.smartcapture.core.toNormalized
import com.videonatics.smartcapture.core.toPixel
import com.videonatics.smartcapture.core.toRect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import javax.inject.Inject

@HiltViewModel
class ImageEditorViewModel @Inject constructor(

) : ViewModel(){

    private val _detectedBox = MutableStateFlow<RectF?>(null)
    val detectedBox: Flow<RectF?> = _detectedBox

    fun precessImage(
        bmp: Bitmap,
        boundBox: RectF,
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
//val imageWidth = prep.srcWidth.toFloat()
//val imageHeight = prep.srcHeight.toFloat()

        if(detection !=null){
            val normBox = detection.toRect().toNormalized(
                image.width.toFloat(),
                image.height.toFloat()
            )

            _detectedBox.value = normBox
        }

//        onImageDimensionsCaptured(imageWidth, imageHeight)

    }

}