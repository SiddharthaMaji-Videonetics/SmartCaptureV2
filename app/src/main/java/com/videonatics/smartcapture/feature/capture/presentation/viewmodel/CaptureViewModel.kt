package com.videonatics.smartcapture.feature.capture.presentation.viewmodel

import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.toRect
import androidx.lifecycle.ViewModel
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.filterInside
import com.videonatics.smartcapture.core.findBestBox
import com.videonatics.smartcapture.core.generateAlignmentVector
import com.videonatics.smartcapture.core.generateInstruction
import com.videonatics.smartcapture.core.mergeToFittedBoxes
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

    private var _inst = MutableStateFlow<AlignmentVector?>(null)
    val inst: Flow<AlignmentVector?> = _inst
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

        val destDetection = findBestBox(_detectionList,_focusBox!!)
        destDetection?.apply {
            val inst = generateInstruction(destDetection,_focusBox!!)

            val hint = generateAlignmentVector(destDetection, _focusBox!!)
            _inst.value = hint
            Log.e("instruction", inst+"($hint)")
        }


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