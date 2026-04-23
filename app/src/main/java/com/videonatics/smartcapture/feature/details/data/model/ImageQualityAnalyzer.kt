package com.videonatics.smartcapture.feature.details.data.model

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object ImageQualityAnalyzer {

    fun analyze(
        bitmap: Bitmap,
        config: ImageQualityConfig = ImageQualityConfig()
    ): ImageQualityResult {

        return try {
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)

            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

            // ---- Blur Detection ----
            val laplacian = Mat()
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)

            val mean = MatOfDouble()
            val stdDev = MatOfDouble()
            Core.meanStdDev(laplacian, mean, stdDev)

            val lapVar = Math.pow(stdDev.get(0, 0)[0], 2.0)
            val isBlurry = lapVar < config.blurThreshold

            // ---- Low Light Detection ----
            val lab = Mat()
            Imgproc.cvtColor(mat, lab, Imgproc.COLOR_BGR2Lab)

            val channels = ArrayList<Mat>()
            Core.split(lab, channels)

            val lChannel = channels[0]
            val lMean = Core.mean(lChannel).`val`[0]

            // Dark pixel %
            val totalPixels = gray.rows() * gray.cols()
            val darkPixels = Core.countNonZero(
                Mat().apply {
                    Core.compare(
                        gray,
                        Scalar(config.darkPixelValue.toDouble()),
                        this,
                        Core.CMP_LT
                    )
                }
            )

            val darkPct = (darkPixels.toDouble() / totalPixels) * 100

            val isUnderlit =
                (lMean < config.lowLightThreshold) ||
                        (darkPct > config.darkPctThreshold)

            ImageQualityResult(
                laplacianVar = lapVar,
                labLMean = lMean,
                darkPixelPct = darkPct,
                isBlurry = isBlurry,
                isUnderlit = isUnderlit
            )

        } catch (e: Exception) {
            ImageQualityResult(error = e.message)
        }
    }
}
