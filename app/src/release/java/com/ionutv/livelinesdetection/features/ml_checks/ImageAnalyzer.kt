package com.ionutv.livelinesdetection.features.ml_checks

import android.app.Application
import com.ionutv.livelinesdetection.features.camera.LivelinessDetectionOption
import kotlinx.coroutines.CoroutineScope

internal class ImageAnalyzer(
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private val detectionOption: LivelinessDetectionOption,
) : ImageAnalyzerCommon(application, viewModelScope, detectionOption)