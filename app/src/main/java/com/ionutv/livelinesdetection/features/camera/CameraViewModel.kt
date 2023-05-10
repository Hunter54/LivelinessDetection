package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ionutv.livelinesdetection.features.facedetection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.facedetection.analyzeImage
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val faceResultFlow = MutableStateFlow<FaceAnalyzerResult>(FaceAnalyzerResult.NoFaceDetected)

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
                    faceResultFlow.value = it
                }
            }
        }
}