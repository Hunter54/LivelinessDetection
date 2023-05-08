package com.ionutv.livelinesdetection.features.facedetection

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

sealed interface FaceAnalyzerResult {
    object MultipleFaceInsideFrame : FaceAnalyzerResult
    object NoFaceDetected : FaceAnalyzerResult
    data class Error(val error: String) : FaceAnalyzerResult
}
data class FaceDetected(val boundaries: Rect, val imageWidth: Int, val imageHeight: Int) : FaceAnalyzerResult

@ExperimentalGetImage
fun analyzeImage(image: ImageProxy, myCode: (FaceAnalyzerResult) -> Unit) {
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
                    myCode(FaceAnalyzerResult.NoFaceDetected)
                    return@addOnSuccessListener
                }
                if (faces.size > 1) {
                    myCode(FaceAnalyzerResult.MultipleFaceInsideFrame)
                    return@addOnSuccessListener
                }
                val face = faces.first()
                val rotationDegrees: Int = inputImage.rotationDegrees
                if (rotationDegrees == 0 || rotationDegrees == 180) {
                    myCode(FaceDetected(face.boundingBox, image.width, image.height))
                } else {
                    myCode(FaceDetected(face.boundingBox, image.height, image.width))
                }
            }
            .addOnFailureListener { e -> // Task failed with an exception
                myCode(FaceAnalyzerResult.Error(e.localizedMessage.toString()))
            }
            .addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                image.close()
            }
    }
}

@ExperimentalGetImage
class FaceDetectionImageAnalyzer : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
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
                    image.close()
                    faces.forEach {
                        val smileProbability = it.smilingProbability
                    }
                }
                .addOnFailureListener(
                    OnFailureListener { e -> // Task failed with an exception
                        e.printStackTrace()
                    })
                .addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must call image.close() on received
                    // images when finished using them. Otherwise, new images may not be received or the camera
                    // may stall.
//                    image.close()
                }

        }

    }
}