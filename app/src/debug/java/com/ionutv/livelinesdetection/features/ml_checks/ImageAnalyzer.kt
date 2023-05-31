package com.ionutv.livelinesdetection.features.ml_checks

import android.app.Application
import android.graphics.BitmapFactory
import com.ionutv.livelinesdetection.R
import kotlinx.coroutines.CoroutineScope

internal class ImageAnalyzer(
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private val detectionOption: LivelinessDetectionOption
) : ImageAnalyzerCommon(application, viewModelScope, detectionOption) {

    init {
        setupFaceRecognitionExistingFaces(application)
    }

    private fun setupFaceRecognitionExistingFaces(application: Application) {
        val ionut = BitmapFactory.decodeResource(application.resources, R.drawable.ionut)
        val paul = BitmapFactory.decodeResource(application.resources, R.drawable.paul)
        val dorian = BitmapFactory.decodeResource(application.resources, R.drawable.dorian)
        val fabian = BitmapFactory.decodeResource(application.resources, R.drawable.fabian)
        addImageToFaceList(ionut, "ionut")
        addImageToFaceList(paul, "paul")
        addImageToFaceList(dorian, "dorian")
        addImageToFaceList(fabian, "fabian")
    }
}