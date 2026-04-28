package com.videonatics.smartcapture.feature.details.data.repository

import android.graphics.Bitmap
import com.videonatics.smartcapture.core.Constants
import com.videonatics.smartcapture.core.MoireEffectClassifier
import com.videonatics.smartcapture.core.data.safeApiCall
import com.videonatics.smartcapture.feature.details.FatBoxClassifier
import com.videonatics.smartcapture.feature.details.data.model.ImageQualityAnalyzer
import com.videonatics.smartcapture.feature.details.data.model.ImageQualityResult
import com.videonatics.smartcapture.feature.details.data.model.ImageUploadResponse
import com.videonatics.smartcapture.feature.details.domain.repository.ImageUploadRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImageUploadRepositoryImpl @Inject constructor(
    private val client: HttpClient,
    private val classifier: FatBoxClassifier,
    private val moireClassifier: MoireEffectClassifier
) : ImageUploadRepository {

    override suspend fun uploadImage(
        stageId: String,
        imageBytes: ByteArray,
        onProgress: (Float) -> Unit
    ): Result<ImageUploadResponse> {
        return safeApiCall(
            call = {
                client.post("${Constants.Endpoints.UPLOAD_IMAGE}$stageId/image") {
                    onUpload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onProgress(bytesSentTotal.toFloat() / contentLength)
                        }
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("image", imageBytes, Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                                })
                            }
                        )
                    )
                }
            },
            map = { it.body<ImageUploadResponse>() }
        )
    }

    override suspend fun analyzeImageQuality(bitmap: Bitmap): Result<ImageQualityResult> = withContext(Dispatchers.Default) {
        try {
            val result = ImageQualityAnalyzer.analyze(bitmap)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun classifyImage(bitmap: Bitmap): Result<FatBoxClassifier.Result> = withContext(Dispatchers.Default) {
        try {
            val result = classifier.classify(bitmap)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyMoireEffect(bitmap: Bitmap): Flow<Boolean> {
        return moireClassifier.analyze(bitmap)
    }
}
