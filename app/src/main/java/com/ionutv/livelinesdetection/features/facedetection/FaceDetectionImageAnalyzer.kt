package com.ionutv.livelinesdetection.features.facedetection

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions


@ExperimentalGetImage
class FaceDetectionImageAnalyzer : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
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
                    image.close()
                }

        }

    }
}