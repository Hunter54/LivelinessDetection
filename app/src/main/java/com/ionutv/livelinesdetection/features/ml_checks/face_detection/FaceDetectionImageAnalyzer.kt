package com.ionutv.livelinesdetection.features.ml_checks.face_detection

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.internal.ImageConvertUtils
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.roundToInt

internal sealed interface FaceDetectionResult {
    object MultipleFaceInsideFrame : FaceDetectionResult
    object NoFaceDetected : FaceDetectionResult
    data class Error(val error: String) : FaceDetectionResult
}

internal data class FaceDetected(
    val boundaries: Rect,
    val image: Bitmap,
    val headAngle: Int,
    val smiling: Boolean? = null,
    val eyesOpen: Boolean? = null,
) : FaceDetectionResult

@ExperimentalGetImage
internal fun detectFace(
    image: ImageProxy,
    allowedFaceWidth: Float = 0.3f,
    detectSmilingOrEyesOpen: Boolean = false,
    onFaceAnalysisResult: (FaceDetectionResult) -> Unit
) {
    val mediaImage = image.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(
                if (detectSmilingOrEyesOpen) FaceDetectorOptions.CLASSIFICATION_MODE_ALL
                else FaceDetectorOptions.CLASSIFICATION_MODE_NONE
            )
            .setMinFaceSize(0.9f)
            .build()

        val detector: FaceDetector = FaceDetection.getClient(options)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onFaceAnalysisResult(FaceDetectionResult.NoFaceDetected)
                    return@addOnSuccessListener
                }
                if (faces.size > 1) {
                    onFaceAnalysisResult(FaceDetectionResult.MultipleFaceInsideFrame)
                    return@addOnSuccessListener
                }
                val face = faces.first()
                val rotationDegrees: Int = inputImage.rotationDegrees
                val bitmapImage = ImageConvertUtils.getInstance().getUpRightBitmap(inputImage)

                fun isFaceWideEnough(
                    rotationDegrees: Int,
                    faceWidth: Float,
                    image: ImageProxy,
                    allowedFaceWidth: Float
                ): Boolean {
                    return if (rotationDegrees == 0 || rotationDegrees == 180) {
                        (faceWidth / image.width.toFloat() > allowedFaceWidth)
                    } else {
                        (faceWidth / image.height.toFloat() > allowedFaceWidth)
                    }
                }

                if (isFaceWideEnough(
                        rotationDegrees,
                        face.boundingBox.width().toFloat(),
                        image,
                        allowedFaceWidth
                    )
                ) {
                    onFaceAnalysisResult(
                        FaceDetected(
                            face.boundingBox,
                            bitmapImage,
                            face.headEulerAngleY.roundToInt()
                        )
                    )

                } else {
                    onFaceAnalysisResult(FaceDetectionResult.NoFaceDetected)
                }
            }
            .addOnFailureListener { e -> // Task failed with an exception
                onFaceAnalysisResult(FaceDetectionResult.Error(e.localizedMessage.toString()))
            }
            .addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                image.close()
            }
    }
}