package com.videonatics.smartcapture.feature.capture.presentation.viewmodel

import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.toRect
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.filterInside
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class CaptureViewModel @Inject constructor() : ViewModel(){
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f
    private var imageWidth: Float = 0f
    private var imageHeight: Float = 0f

    private val _detectionList = ArrayList<Detection>()
    private var _focusBox: RectF? =null
    private var _objectInsideFocusBoxCount = MutableStateFlow(0)
    val objectInsideFocusBoxCount: Flow<Int> = _objectInsideFocusBoxCount
    fun setDetection(detections: List<Detection>) {
        _detectionList.clear()
        _detectionList.addAll(detections)

        calculateObjectInsideFocusBoxCount() //update objects that inside focus box
    }

    private fun calculateObjectInsideFocusBoxCount() {
        if(_focusBox == null) return
        if(_detectionList.isEmpty()) return

        Log.e("box1", _focusBox.toString())
        Log.e("box2", _detectionList.map { it.rect }.toString())

        val insideRects = _detectionList.map { it.rect.toRect() }.filterInside(_focusBox?.toRect()!!)


        _objectInsideFocusBoxCount.tryEmit(insideRects.size)
            Log.e("boxInBox", insideRects.size.toString())

    }

    fun setFocusBox(rect: RectF){
        _focusBox = rect
    }

    fun updateCanvasSize(w: Float, h: Float) {
        if (canvasWidth == w && canvasHeight == h) return

        canvasWidth = w
        canvasHeight = h
    }

}