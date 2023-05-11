package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ionutv.livelinesdetection.features.emotion_detection.ClassifierService
import com.ionutv.livelinesdetection.features.face_detection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.face_detection.analyzeImage
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.tensorflow.lite.support.common.ops.NormalizeOp

@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val faceResultFlow = MutableStateFlow<FaceAnalyzerResult>(FaceAnalyzerResult.NoFaceDetected)

    private val emotionClassifier =
        ClassifierService(
            application,
            "emotion_model.tflite",
            "emotion_label.txt",
            NormalizeOp(0f, 255f),
            NormalizeOp(0.0f, 1.0f)
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
                        val bitmap = cropBitmap(it.image, it.boundaries)
                        val result = emotionClassifier.processImage(bitmap)
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
    val w = rect.right - rect.left
    val h = rect.bottom - rect.top
    return Bitmap.createBitmap(w, h, bitmap.config)
}