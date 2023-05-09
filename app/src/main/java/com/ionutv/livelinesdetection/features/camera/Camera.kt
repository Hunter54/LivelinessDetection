package com.ionutv.livelinesdetection.features.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.HIGH_RESOLUTION_FLAG_ON
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.ionutv.livelinesdetection.features.DrawFaceDetection
import com.ionutv.livelinesdetection.features.facedetection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.facedetection.analyzeImage
import com.ionutv.livelinesdetection.utils.executor
import com.ionutv.livelinesdetection.utils.getCameraProvider
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraPreview(
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
fun CameraCapture(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    onImageFile: (File) -> Unit = { }
) {
    val coroutineScope = rememberCoroutineScope()
    val imageCaptureUseCase by remember {
        mutableStateOf(
            ImageCapture.Builder().setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build()
        )
    }
    var previewUseCase by remember { mutableStateOf<UseCase>(Preview.Builder().build()) }
    var faceImage: FaceAnalyzerResult by remember {
        mutableStateOf(FaceAnalyzerResult.NoFaceDetected)
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DrawFaceDetection( modifier = modifier,
        onPreviewUseCase = {
            previewUseCase = it
        },
        faceImage = faceImage,
        onImageCapture = {
            coroutineScope.launch {
                onImageFile(
                    imageCaptureUseCase.takePicture(
                        context.executor
                    )
                )
            }
        }
    )
    LaunchedEffect(previewUseCase) {
        val cameraProvider = context.getCameraProvider()
        try {
            val imageAnalysisUseCase = ImageAnalysis.Builder()
                // enable the following line if RGBA output is needed.
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setHighResolutionEnabledFlag(HIGH_RESOLUTION_FLAG_ON).build()
                    )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(context.executor) {imageProxy ->
                        analyzeImage(imageProxy) {
                            faceImage = it
                        }

                    }
                }

            // Must unbind the use-cases before rebinding them.
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase,
                imageAnalysisUseCase
            )
        } catch (ex: Exception) {
            Log.e("CameraCapture", "Failed to bind camera use cases", ex)
        }
    }

}