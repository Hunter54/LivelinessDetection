package com.ionutv.livelinesdetection.features.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    Box(modifier = modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var previewUseCase by remember { mutableStateOf<UseCase>(Preview.Builder().build()) }
        Box {
            CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                previewUseCase = it
            })
            Button(modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
                onClick = { coroutineScope.launch { imageCaptureUseCase.takePicture(context.executor).let {
                    onImageFile(it)
                } } })
            {
                Text("Click!")
            }
        }
        LaunchedEffect(previewUseCase) {
            val cameraProvider = context.getCameraProvider()
            try {
                // Must unbind the use-cases before rebinding them.
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, previewUseCase, imageCaptureUseCase
                )
            } catch (ex: Exception) {
                Log.e("CameraCapture", "Failed to bind camera use cases", ex)
            }
        }
    }
}