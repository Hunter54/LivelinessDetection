package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ionutv.livelinesdetection.features.ml_checks.ClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.ImageAnalyzer
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn


@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val emotionClassifier = EmotionImageClassifier(application)

    private val faceNetFaceRecognition = FaceNetFaceRecognition(application)

    private val imageAnalyzer =
        ImageAnalyzer(application, viewModelScope, emotionClassifier, faceNetFaceRecognition)

    val cameraProviderFlow = flow {
        application.getCameraProvider().also {
            emit(it)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val faceResultFlow = imageAnalyzer.resultFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        initialValue = FaceClassifierResult.NoFaceDetected
    )

    val imageAnalysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(application.executor, imageAnalyzer)
        }

    override fun onCleared() {
        emotionClassifier.closeResource()
        super.onCleared()
    }
}

internal sealed interface FaceClassifierResult {
    object NoFaceDetected : FaceClassifierResult
    data class Error(val message: String) : FaceClassifierResult

    data class FaceClassified(
        val boundaries: Rect,
        val image: Bitmap,
        val croppedImage: Bitmap,
        val emotions: List<ClassifierResult>
    ) : FaceClassifierResult

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