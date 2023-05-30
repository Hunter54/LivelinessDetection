package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class CameraViewModelFactory(
    private val application: Application,
    private val livelinessDetectionOption: LivelinessDetectionOption
) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CameraViewModel(application, livelinessDetectionOption) as T
}