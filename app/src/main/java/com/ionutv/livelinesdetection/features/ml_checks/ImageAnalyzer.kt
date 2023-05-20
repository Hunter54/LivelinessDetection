package com.ionutv.livelinesdetection.features.ml_checks

import android.content.Context
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.ionutv.livelinesdetection.features.camera.FaceClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetectionResult
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.analyzeImage
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class ImageAnalyzer(
    private val context: Context,
    private val viewModelScope: CoroutineScope,
    private val emotionImageClassifier: EmotionImageClassifier,
    private val faceNetFaceRecognition: FaceNetFaceRecognition
) : ImageAnalysis.Analyzer {

    private val _resultFlow = MutableSharedFlow<FaceClassifierResult>()
    val resultFlow = _resultFlow.asSharedFlow()

    private var isProcessing = false

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        isProcessing = true
        analyzeImage(image) {
            viewModelScope.launch {
                when (it) {
                    is FaceDetectionResult.Error -> {
                        _resultFlow.emit(FaceClassifierResult.Error(it.error))
                    }

                    is FaceDetected -> {
                        analyzeFace(it)
                    }

                    FaceDetectionResult.MultipleFaceInsideFrame -> {
                        _resultFlow.emit(FaceClassifierResult.Error("Multiple faces in frame"))
                    }

                    FaceDetectionResult.NoFaceDetected -> {
                        _resultFlow.emit(FaceClassifierResult.NoFaceDetected)
                    }
                }
                isProcessing = false
            }
        }
    }

    private suspend fun analyzeFace(it: FaceDetected) {
        val croppedBitmap =
            ImageClassifierService.cropBitmapExample(it.image, it.boundaries)
        val result = emotionImageClassifier.classifyEmotions(croppedBitmap)
        if (result.isNotEmpty()) {
            Log.d("EMOTION_CLASSIFIER IS", result.first().toString())
        }
        _resultFlow.emit(
            FaceClassifierResult.FaceClassified(
                it.boundaries,
                image = it.image,
                croppedImage = croppedBitmap,
                emotions = result
            )
        )
        //val embedding = faceNetFaceRecognition.processImage(croppedBitmap)
    }
}