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
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.ImageAnalyzer
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.VerificationState
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalGetImage::class)
internal class CameraViewModel(
    private val application: Application,
    isDebugMode: Boolean
) :
    AndroidViewModel(application) {


    private val _detectionOption = MutableStateFlow(LivelinessDetectionOption.SMILE)
    val detectionOption = _detectionOption.asStateFlow()

    private var imageAnalyzer =
        ImageAnalyzer(
            application, viewModelScope,
            _detectionOption.value,
            isDebugMode
        )

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    internal val verificationState = _detectionOption.flatMapLatest { latestOption ->
        imageAnalyzer.changeDetectionOption(latestOption)
        imageAnalyzer.verificationState
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VerificationState.Start)

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
            setAnalyzer(application.executor) {
                imageAnalyzer.analyzeImage(it)
            }
        }

    init {
        viewModelScope.launch {
            verificationState.collect {
                Log.d("VIEWMODEL TEST", it.toString())
            }
        }
    }

    fun updateDetectionOption(detectionOption: LivelinessDetectionOption) {
        _detectionOption.update {
            detectionOption
        }
    }

    fun restartFlow() {
        imageAnalyzer.resetFlow()
    }

    override fun onCleared() {
        Log.d("VIEWMODEL TEST", "onCleared")
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