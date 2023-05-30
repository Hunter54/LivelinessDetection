package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
public fun DetectionAndCameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    livelinessDetectionOption: LivelinessDetectionOption = LivelinessDetectionOption.ANGLED_FACES_WITH_SMILE
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val cameraViewModel: CameraViewModel =
        viewModel(factory = CameraViewModelFactory(application, livelinessDetectionOption))
    val faceImage: FaceClassifierResult by cameraViewModel.faceResultFlow.collectAsState()

    CameraPreviewAndFaceHighlight(
        modifier = modifier,
        cameraSelector = cameraSelector,
        cameraViewModel = cameraViewModel,
        classifiedFace = faceImage,
    )
}

@Composable
internal fun PreviewAndUserGuidance(
    cameraSelector: CameraSelector,
    cameraViewModel: CameraViewModel,
    classifiedFace: FaceClassifierResult,
    modifier: Modifier = Modifier
) {
    val cameraProvider by cameraViewModel.cameraProviderFlow.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    key(cameraProvider) {
        Box(modifier = modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                    cameraProvider?.apply {
                        // Must unbind the use-cases before rebinding them.
                        unbindAll()
                        bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            it,
                            cameraViewModel.imageAnalysisUseCase
                        )
                    }
                })
                val textMeasurer = rememberTextMeasurer()
                Canvas(modifier = modifier.fillMaxSize()) {
                    val ovalWidth = maxWidth.toPx() / 1.8f
                    val ovalHeight = maxHeight.toPx() / 2.3f
                    val ovalX = maxWidth.toPx() / 2f - ovalWidth / 2f
                    val ovalY = maxHeight.toPx() / 2f - ovalHeight / 2f

                    drawOval(
                        Color.Gray,
                        size = Size(width = ovalWidth, height = ovalHeight),
                        topLeft = Offset(x = ovalX, y = ovalY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    when (classifiedFace) {
                        is FaceClassifierResult.FaceClassified -> {
                        }

                        is FaceClassifierResult.Error -> {

                        }

                        FaceClassifierResult.NoFaceDetected -> {

                        }
                    }

                }
            }
        }
    }
}