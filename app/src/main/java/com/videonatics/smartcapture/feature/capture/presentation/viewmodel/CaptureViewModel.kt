package com.videonatics.smartcapture.feature.capture.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.Color
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

    private val _detectionList = ArrayList<Detection>()
    private var _focusBox: RectF? =null
    private var _objectInsideFocusBoxCount = MutableStateFlow(0)

    private var _inst = MutableStateFlow<AlignmentVector?>(null)
    val inst: Flow<AlignmentVector?> = _inst

    private var _focusedObj = MutableStateFlow<Detection?>(null)
    val focusedObj : Flow<Detection?> = _focusedObj

    private var _isCapturable = MutableStateFlow<Boolean>(false)
    var isCapturable = _isCapturable

    private var _captureTrigger = MutableStateFlow<Boolean>(false)
    var captureTrigger = _captureTrigger




    fun setDetection(detections: List<Detection>) {

        _detectionList.clear()
        _detectionList.addAll(detections)

        calculateObjectInsideFocusBoxCount() //update objects that inside focus box
    }

    private fun calculateObjectInsideFocusBoxCount() {

        val focus = _focusBox ?: return
        if (_detectionList.isEmpty()) return

        // --- Helper: corners → RectF ---
        fun Detection.toRect(): RectF {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (p in corners) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
            return RectF(minX, minY, maxX, maxY)
        }

        Log.e("box1", focus.toString())
        Log.e("box2", _detectionList.map { it.toRect() }.toString())

        // --- Best detection ---
        val best = findBestBox(_detectionList, focus)

        best?.let { det ->

            // 🔥 FIX: store Detection, NOT rect
            _focusedObj.value = det

            val inst = generateInstruction(det, focus)
            val hint = generateAlignmentVector(det, focus)

            _isCapturable.value =
                hint.z in -1.5f..0f &&
                        hint.h in -0.02f..0.02f &&
                        hint.v in -0.02f..0.02f

            _inst.value = hint

            Log.e("instruction", "$inst ($hint)")
        }

        // --- Count objects inside focus box ---
        val insideRects = _detectionList
            .map { it.toRect() }
            .filterInside(focus)

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

    fun captureImage() {
        _captureTrigger.value = true
    }

    fun captureReset() {
        _captureTrigger.value = false
    }

}