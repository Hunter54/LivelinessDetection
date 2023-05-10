package com.ionutv.livelinesdetection.features.camera

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionutv.livelinesdetection.features.CameraPreviewAndFaceHighlight
import com.ionutv.livelinesdetection.features.facedetection.FaceAnalyzerResult

@Composable
internal fun CameraPreview(
    modifier: Modifier = Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    onUseCase: (UseCase) -> Unit = { }
) {
    AndroidView(modifier = modifier, factory = { context ->
        val previewView = PreviewView(context).apply {
            this.scaleType = scaleType
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        onUseCase(Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        })
        previewView
    })
}

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