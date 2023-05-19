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
import com.ionutv.livelinesdetection.features.ClassifierResult
import com.ionutv.livelinesdetection.features.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.emotion_detection.EmotionImageClassifier.Companion.cropBitmapExample
import com.ionutv.livelinesdetection.features.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.face_detection.FaceDetectionResult
import com.ionutv.livelinesdetection.features.face_detection.analyzeImage
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn


@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val faceResultFlow = MutableStateFlow<FaceClassifierResult>(FaceClassifierResult.NoFaceDetected)

    private val emotionClassifier = EmotionImageClassifier(application)


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
                    when (it) {
                        is FaceDetectionResult.Error -> {
                            faceResultFlow.value = FaceClassifierResult.Error(it.error)
                        }

                        is FaceDetected -> {
                            val croppedBitmap = cropBitmapExample(it.image, it.boundaries)
                            val result = emotionClassifier.classifyEmotions(croppedBitmap)
//                            val test = faceRecognitionClassifier.processImage(croppedBitmap)
                            if (result.isNotEmpty()) {
                                Log.d("EMOTION_CLASSIFIER IS", result.first().toString())
                            }
                            faceResultFlow.value = FaceClassifierResult.FaceClassified(
                                it.boundaries,
                                image = it.image,
                                croppedImage = croppedBitmap,
                                emotions = result
                            )
                        }

                        FaceDetectionResult.MultipleFaceInsideFrame -> {
                            faceResultFlow.value =
                                FaceClassifierResult.Error("Multiple faces in frame")
                        }

                        FaceDetectionResult.NoFaceDetected -> {
                            faceResultFlow.value = FaceClassifierResult.NoFaceDetected
                        }
                    }

                }

            }
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