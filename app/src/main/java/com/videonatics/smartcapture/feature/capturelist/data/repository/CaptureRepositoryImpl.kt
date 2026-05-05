package com.videonatics.smartcapture.feature.capturelist.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import com.videonatics.smartcapture.core.data.local.dao.CaptureDao
import com.videonatics.smartcapture.core.data.local.entity.CaptureEntity
import com.videonatics.smartcapture.core.domain.model.CaptureModel
import com.videonatics.smartcapture.feature.capturelist.domain.repository.CaptureRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class CaptureRepositoryImpl @Inject constructor(
    private val captureDao: CaptureDao,
    @ApplicationContext private val context: Context
) : CaptureRepository {

    override fun getAllCaptures(): Flow<List<CaptureModel>> {
        return captureDao.getAllCaptures().map { entities ->
            entities.map { entity ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
                val bitmap = BitmapFactory.decodeFile(entity.imagePath, options)
                
                val corners = entity.corners?.let { cornersStr ->
                    try {
                        cornersStr.split("|").map { pointStr ->
                            val parts = pointStr.split(",")
                            PointF(parts[0].toFloat(), parts[1].toFloat())
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                CaptureModel(
                    id = entity.id,
                    image = bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    timestamp = entity.timestamp,
                    boundBox = if (entity.left != null && entity.top != null && entity.right != null && entity.bottom != null) {
                        RectF(entity.left, entity.top, entity.right, entity.bottom)
                    } else null,
                    obbCorners = corners,
                    rotation = entity.rotation ?: 0f,
                    stageId = entity.stageId
                )
            }
        }
    }

    override suspend fun saveCapture(capture: CaptureModel) {
        withContext(Dispatchers.IO) {
            val imagePath = saveBitmapToFile(capture.image, capture.id)
            
            val cornersStr = capture.obbCorners?.joinToString("|") { "${it.x},${it.y}" }

            val entity = CaptureEntity(
                id = capture.id,
                imagePath = imagePath,
                timestamp = capture.timestamp,
                left = capture.boundBox?.left,
                top = capture.boundBox?.top,
                right = capture.boundBox?.right,
                bottom = capture.boundBox?.bottom,
                rotation = capture.rotation,
                corners = cornersStr,
                stageId = capture.stageId
            )
            captureDao.insertCapture(entity)
        }
    }

    override suspend fun deleteCapture(id: String) {
        withContext(Dispatchers.IO) {
            val capture = captureDao.getCaptureById(id)
            capture?.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            captureDao.deleteCapture(id)
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, filename: String): String {
        val directory = File(context.filesDir, "captures")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        // Using PNG for lossless storage to maintain maximum quality
        val file = File(directory, "$filename.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}
