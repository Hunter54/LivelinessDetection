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

internal sealed interface FaceDetectionResult {
    object MultipleFaceInsideFrame : FaceDetectionResult
    object NoFaceDetected : FaceDetectionResult
    data class Error(val error: String) : FaceDetectionResult
}

internal data class FaceDetected(
    val boundaries: Rect,
    val image: Bitmap
) : FaceDetectionResult

@ExperimentalGetImage
internal inline fun analyzeImage(
    image: ImageProxy,
    allowedFaceWidth: Float = 0.3f,
    crossinline onFaceAnalysisResult: (FaceDetectionResult) -> Unit
) {
    val mediaImage = image.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
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
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    if (face.boundingBox.width().toFloat() / image.width.toFloat() < 0.30) {
                        onFaceAnalysisResult(FaceDetectionResult.NoFaceDetected)
                        return@addOnSuccessListener
                    }
                    onFaceAnalysisResult(
                        FaceDetected(
                            face.boundingBox,
                            bitmapImage
                        )
                    )
                } else {
                    if (face.boundingBox.width().toFloat() / image.height.toFloat() < 0.30) {
                        onFaceAnalysisResult(FaceDetectionResult.NoFaceDetected)
                        return@addOnSuccessListener
                    }
                    onFaceAnalysisResult(
                        FaceDetected(
                            face.boundingBox,
                            bitmapImage
                        )
                    )
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

//@ExperimentalGetImage
//class FaceDetectionImageAnalyzer : ImageAnalysis.Analyzer {
//
//    override fun analyze(image: ImageProxy) {
//        val mediaImage = image.image
//        if (mediaImage != null) {
//            val inputImage =
//                InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
//            val options = FaceDetectorOptions.Builder()
//                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
//                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
//                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
//                .setMinFaceSize(0.9f)
//                .build()
//
//            val detector: FaceDetector = FaceDetection.getClient(options)
//            detector.process(inputImage)
//                .addOnSuccessListener { faces ->
//                    image.close()
//                    faces.forEach {
//                        val smileProbability = it.smilingProbability
//                    }
//                }
//                .addOnFailureListener(
//                    OnFailureListener { e -> // Task failed with an exception
//                        e.printStackTrace()
//                    })
//                .addOnCompleteListener {
//                    // When the image is from CameraX analysis use case, must call image.close() on received
//                    // images when finished using them. Otherwise, new images may not be received or the camera
//                    // may stall.
////                    image.close()
//                }
//
//        }
//
//    }
//}