package com.videonatics.smartcapture.feature.capture.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toRect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videonatics.smartcapture.core.LevelSensorHelper
import com.videonatics.smartcapture.core.domain.model.AlignmentVector
import com.videonatics.smartcapture.core.domain.model.Detection
import com.videonatics.smartcapture.core.filterInside
import com.videonatics.smartcapture.core.findBestBox
import com.videonatics.smartcapture.core.generateAlignmentVector
import com.videonatics.smartcapture.core.generateInstruction
import com.videonatics.smartcapture.core.mergeToFittedBoxes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        // Tolerance values — widen these to make capture easier
        const val CAPTURABLE_Z_MIN = -1.5f
        const val CAPTURABLE_Z_MAX = 0f
        const val CAPTURABLE_H_TOLERANCE = 0.05f  // ±5 % horizontal
        const val CAPTURABLE_V_TOLERANCE = 0.05f  // ±5 % vertical
    }

    // --- Capturable tolerance (adjust at runtime to ease/tighten detection) ---
    var capturableZMin: Float = CAPTURABLE_Z_MIN
    var capturableZMax: Float = CAPTURABLE_Z_MAX
    var capturableHTolerance: Float = CAPTURABLE_H_TOLERANCE
    var capturableVTolerance: Float = CAPTURABLE_V_TOLERANCE

    // --- Audio prompt ---
    private val _isAudioPrompt = MutableStateFlow(true)
    val isAudioPrompt: StateFlow<Boolean> = _isAudioPrompt

    fun setAudioPrompt(enabled: Boolean) { _isAudioPrompt.value = enabled }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var wasCapturable = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.getDefault()
                val result = tts?.setLanguage(locale)
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    private fun speakIfReady() {
        if (!_isAudioPrompt.value || !isTtsReady) return
        val prompt = when (Locale.getDefault().language) {
            "hi" -> "कैप्चर के लिए तैयार"
            "es" -> "Listo para capturar"
            "fr" -> "Prêt à capturer"
            "de" -> "Bereit zum Aufnehmen"
            "zh" -> "准备拍摄"
            "ja" -> "撮影準備完了"
            "ko" -> "촬영 준비 완료"
            "ar" -> "جاهز للتصوير"
            "pt" -> "Pronto para capturar"
            else -> "Ready to capture"
        }
        tts?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, "capture_ready")
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }

    // -------------------------------------------------------------------------

    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f

    private val _detectionList = ArrayList<Detection>()
    private var _focusBox: RectF? =null
    private var _objectInsideFocusBoxCount = MutableStateFlow(0)
    val detectedObjCount = _objectInsideFocusBoxCount

    private var _inst = MutableStateFlow<AlignmentVector?>(null)
    val inst: Flow<AlignmentVector?> = _inst

    private var _focusedObj = MutableStateFlow<Detection?>(null)
    val focusedObj : Flow<Detection?> = _focusedObj

    private var _isCapturable = MutableStateFlow<Boolean>(false)
    var isCapturable = _isCapturable

    private var _captureTrigger = MutableStateFlow<Boolean>(false)
    var captureTrigger = _captureTrigger

    private val _isLevel = MutableStateFlow(false)
    fun updateLevel(level: Boolean) { _isLevel.value = level }

    private val _isBoundBoxEdit = MutableStateFlow(false)
    fun updateBoundBoxEdit(editing: Boolean) { _isBoundBoxEdit.value = editing }

    //stability ----

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable

    fun updateStability(stable: Boolean) {
        _isStable.value = stable
    }
    //--------------


    fun setDetection(detections: List<Detection>) {

        _detectionList.clear()
        _detectionList.addAll(detections)

        calculateObjectInsideFocusBoxCount() //update objects that inside focus box
    }

    private fun calculateObjectInsideFocusBoxCount() {

        val focus = _focusBox ?: return

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

            val capturable = hint.z in capturableZMin..capturableZMax &&
                    hint.h in -capturableHTolerance..capturableHTolerance &&
                    hint.v in -capturableVTolerance..capturableVTolerance

            val captureButtonVisible = capturable && _isLevel.value && !_isBoundBoxEdit.value
            if (captureButtonVisible && !wasCapturable) speakIfReady()
            wasCapturable = captureButtonVisible
            _isCapturable.value = capturable

            _inst.value = hint

            Log.e("instruction", "$inst ($hint)")
        }

        // --- Count objects inside focus box ---
//        val insideRects = _detectionList
//            .map { it.toRect() }
//            .filterInside(focus)

        _objectInsideFocusBoxCount.tryEmit(_detectionList.size)

      //  Log.e("boxInBox", insideRects.size.toString())
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