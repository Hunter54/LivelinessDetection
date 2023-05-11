package com.ionutv.livelinesdetection.features.camera

import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionutv.livelinesdetection.features.face_detection.FaceAnalyzerResult

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
public fun DetectionAndCameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
) {
    val cameraViewModel: CameraViewModel = viewModel()
    val faceImage: FaceAnalyzerResult by cameraViewModel.faceResultFlow.collectAsState()

    CameraPreviewAndFaceHighlight(
        modifier = modifier,
        cameraSelector = cameraSelector,
        cameraViewModel = cameraViewModel,
        faceImage = faceImage,
    )
}