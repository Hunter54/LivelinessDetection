package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption

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
        viewModel(factory = CameraViewModelFactory(application, livelinessDetectionOption, false))
    val faceImage: FaceClassifierResult by cameraViewModel.faceResultFlow.collectAsState()
    val cameraProvider by cameraViewModel.cameraProviderFlow.collectAsState()

//    CameraPreviewAndFaceHighlight(
//        modifier = modifier,
//        cameraSelector = cameraSelector,
//        cameraProvider = cameraProvider,
//        imageAnalysisUseCase = cameraViewModel.imageAnalysisUseCase,
//        classifiedFace = faceImage
//    )

    PreviewAndUserGuidance(
        modifier = modifier,
        userState = UserState.START,
        cameraSelector = cameraSelector,
        cameraProvider = cameraProvider,
        imageAnalysisUseCase = cameraViewModel.imageAnalysisUseCase
    )
}

@Composable
internal fun PreviewAndUserGuidance(
    cameraSelector: CameraSelector,
    userState: UserState,
    modifier: Modifier = Modifier,
    cameraProvider: ProcessCameraProvider?,
    imageAnalysisUseCase: ImageAnalysis
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    key(cameraProvider) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreview( onUseCase = {
                cameraProvider?.apply {
                    // Must unbind the use-cases before rebinding them.
                    unbindAll()
                    bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        it,
                        imageAnalysisUseCase
                    )
                }
            }, modifier = Modifier
                .fillMaxSize()
                .drawWithContent {

                    val ovalWidth = size.width / 1.8f
                    val ovalHeight = size.height / 2.3f
                    val ovalX = size.width / 2f - ovalWidth / 2f
                    val ovalY = size.height / 2f - ovalHeight / 2f

                    drawContent()

                    drawRect(Color(0x99000000))
                    drawOval(
                        Color.Transparent,
                        size = Size(width = ovalWidth, height = ovalHeight),
                        topLeft = Offset(x = ovalX, y = ovalY),
                        blendMode = BlendMode.SrcIn
                    )
                    drawOval(
                        Color.Gray,
                        size = Size(width = ovalWidth, height = ovalHeight),
                        topLeft = Offset(x = ovalX, y = ovalY),
                        style = Stroke(width = 2.dp.toPx())
                    )

                })
            Canvas(modifier = modifier.fillMaxSize()) {
                when(userState){
                    UserState.END -> TODO()
                    is UserState.Guiding -> TODO()
                    UserState.START -> {

                    }
                }

            }
        }
    }
}