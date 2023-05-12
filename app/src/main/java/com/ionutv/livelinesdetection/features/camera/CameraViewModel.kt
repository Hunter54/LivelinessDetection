package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ionutv.livelinesdetection.features.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.face_detection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.face_detection.analyzeImage
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val faceResultFlow = MutableStateFlow<FaceAnalyzerResult>(FaceAnalyzerResult.NoFaceDetected)

    private val emotionClassifier = EmotionImageClassifier(
        application,
        "emotion_model.tflite",
        "emotion_label.txt",
    )


    val cameraProviderFlow = flow {
        application.getCameraProvider().also {
            emit(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    val imageAnalysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(application.executor) { imageProxy ->
                analyzeImage(imageProxy) {
                    if (it is FaceDetected) {
                        val croppedBitmap = cropBitmap(it.image, it.boundaries)
                        val result = emotionClassifier.processImage(croppedBitmap)
                        if (result.isNotEmpty()) {
                            Log.d("EMOTION_CLASSIFIER IS", result.first().toString())
                        }
                    }
                    faceResultFlow.value = it
                }

            }
        }
}

internal fun cropBitmap(
    bitmap: Bitmap,
    rect: Rect
): Bitmap {
    return Bitmap.createBitmap(
        bitmap,
        rect.left,
        rect.top,
        rect.width(),
        rect.height()
    )
}