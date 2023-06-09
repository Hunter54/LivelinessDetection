package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption

internal class CameraViewModelFactory(
    private val application: Application,
    private val livelinessDetectionOption: LivelinessDetectionOption,
    private val isDebugMode: Boolean
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CameraViewModel(application, livelinessDetectionOption, isDebugMode) as T
}