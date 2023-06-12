package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.VerificationState

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
internal fun DetectionAndCameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    livelinessDetectionOption: LivelinessDetectionOption = LivelinessDetectionOption.ANGLED_FACES,
    cameraViewModel: CameraViewModel
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val faceImage: FaceClassifierResult by cameraViewModel.faceResultFlow.collectAsState()
    val cameraProvider by cameraViewModel.cameraProviderFlow.collectAsState()
    val verificationState by cameraViewModel.verificationState.collectAsState()

//    CameraPreviewAndFaceHighlight(
//        modifier = modifier,
//        cameraSelector = cameraSelector,
//        cameraProvider = cameraProvider,
//        imageAnalysisUseCase = cameraViewModel.imageAnalysisUseCase,
//        classifiedFace = faceImage
//    )
    CameraPreviewWithAreaMarker(
        modifier = modifier,
        cameraSelector = cameraSelector,
        cameraProvider = cameraProvider,
        imageAnalysisUseCase = cameraViewModel.imageAnalysisUseCase
    )
    UserGuidance(
        verificationState,
        onErrorAlertDismissed = { cameraViewModel.restartFlow() },
        onSuccessAlertDismissed = { cameraViewModel.restartFlow() })
}

@Composable
private fun UserGuidance(
    verificationState: VerificationState,
    onErrorAlertDismissed: () -> Unit,
    onSuccessAlertDismissed: () -> Unit
) {

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.3f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        when (val verificationState = verificationState) {
            is VerificationState.Error -> {
                var shouldShowDialog by remember {
                    mutableStateOf(true)
                }
                if (shouldShowDialog) {
                    AlertDialog(
                        onDismissRequest = { shouldShowDialog = false },
                        confirmButton = {
                            Button(onClick = {
                                onErrorAlertDismissed()
                                shouldShowDialog = false
                            }) {
                                Text(text = "OK")
                            }
                        },
                        title = { Text("Error") },
                        text = { Text(verificationState.message) })
                }
            }

            VerificationState.Finished -> {
                var shouldShowDialog by remember {
                    mutableStateOf(true)
                }
                if (shouldShowDialog) {
                    AlertDialog(
                        onDismissRequest = { shouldShowDialog = false },
                        confirmButton = {
                            Button(onClick = {
                                onSuccessAlertDismissed()
                                shouldShowDialog = false
                            }) {
                                Text(text = "OK")
                            }
                        },
                        title = { Text("Successfully verified") },
                        text = { Text("You have successfully verified your account") })
                }
            }

            VerificationState.Start -> {
                //TODO
            }

            is VerificationState.Working -> {
                Text(
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color.Blue,
                            blurRadius = 16f,
                            offset = Offset(0f, 0f)
                        )
                    ), text = verificationState.message
                )
            }
        }
    }
}

@Composable
internal fun CameraPreviewWithAreaMarker(
    cameraSelector: CameraSelector,
    modifier: Modifier = Modifier,
    cameraProvider: ProcessCameraProvider?,
    imageAnalysisUseCase: ImageAnalysis
) {
    Log.d("COMPOSE TEST", "RECOMPOSING camera preview")

    Box(modifier = modifier.fillMaxSize()) {
        key(cameraProvider) {
            val lifecycleOwner = LocalLifecycleOwner.current
            CameraPreview(onUseCase = {
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
        }
    }
}
