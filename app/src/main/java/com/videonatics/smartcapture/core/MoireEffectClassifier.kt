package com.videonatics.smartcapture.core

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.tensorflow.lite.Interpreter

class MoireEffectClassifier(private val interpreter: Interpreter) {

    fun analyze(bitmap: Bitmap): Flow<Boolean> = flow {
        val analyzer = MoireAnalyzer(interpreter)
        val prob = analyzer.analyze(bitmap)
        emit(prob > 0.5f)
    }.flowOn(Dispatchers.Default)
}
