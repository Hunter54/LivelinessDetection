package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.ImageAnalyzer
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(
    application: Application,
    detectionOption: LivelinessDetectionOption
) :
    AndroidViewModel(application) {

    private val imageAnalyzer =
        ImageAnalyzer(
            application, viewModelScope,
            detectionOption
        )

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
        imageAnalyzer.closeResources()
        super.onCleared()
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