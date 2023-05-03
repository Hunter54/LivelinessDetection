package com.ionutv.livelinesdetection.features

import androidx.camera.core.UseCase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ionutv.livelinesdetection.features.camera.CameraPreview
import com.ionutv.livelinesdetection.features.facedetection.FaceAnalyzerResult

@Composable
fun DrawFaceDetection(
    onPreviewUseCase: (UseCase) -> Unit,
    faceCoordinates: FaceAnalyzerResult,
    modifier: Modifier = Modifier,
    onImageCapture: () -> Unit = {},
) {
    Box(modifier = modifier) {
        Box {
            CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                onPreviewUseCase(it)
            })
            Canvas(modifier = modifier.fillMaxSize()) {
                when (faceCoordinates) {
                    is FaceAnalyzerResult.FaceDetected -> {
                        drawRect(
                            color = Color.Yellow,
                            size = Size(
                                faceCoordinates.boundaries.width().dp.toPx(),
                                faceCoordinates.boundaries.height().dp.toPx()
                            ),
                            topLeft = Offset(
                                faceCoordinates.boundaries.left.dp.toPx(),
                                faceCoordinates.boundaries.top.dp.toPx()
                            ),
                            style = Stroke(width = 6.dp.toPx())
                        )
                    }

                    is FaceAnalyzerResult.Error -> {

                    }

                    FaceAnalyzerResult.MultipleFaceInsideFrame -> {

                    }

                    FaceAnalyzerResult.NoFaceDetected -> {

                    }
                }

            }
            Button(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                onClick = onImageCapture
            )
            {
                Text("Click!")
            }
        }
    }
}