package com.ionutv.livelinesdetection.features

import androidx.camera.core.UseCase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
    faceImage: FaceAnalyzerResult,
    modifier: Modifier = Modifier,
    onImageCapture: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                onPreviewUseCase(it)
            })
            Canvas(modifier = modifier.fillMaxSize()) {
                when (faceImage) {
                    is FaceAnalyzerResult.FaceDetected -> {
                        val maxWidthPx = maxWidth.toPx()
                        val maxHeightPx = maxHeight.toPx()
                        val viewAspectRatio = maxWidthPx / maxHeightPx
                        val imageAspectRatio = faceImage.imageWeidth.toFloat() / faceImage.imageHeight.toFloat()
                        val scaleFactor: Float
                        var postScaleHeightOffset: Float = 0f
                        var postScaleWidthOffset: Float = 0f

                        if (viewAspectRatio > imageAspectRatio) {
                            // The image needs to be vertically cropped to be displayed in this view.
                            scaleFactor = maxWidthPx / faceImage.imageWeidth
                            postScaleHeightOffset =
                                (maxWidthPx / imageAspectRatio - maxHeightPx) / 2
                        } else {
                            // The image needs to be horizontally cropped to be displayed in this view.
                            scaleFactor = maxHeightPx / faceImage.imageHeight
                            postScaleWidthOffset =
                                (maxHeightPx * imageAspectRatio - maxWidthPx) / 2
                        }
                        // Translate X and Y coordinates to match image scaled for screen
                        val scaledX = maxWidthPx - (faceImage.boundaries.left * scaleFactor + postScaleWidthOffset)
                        val scaledY = faceImage.boundaries.top * scaleFactor - postScaleHeightOffset
                        drawRect(
                            color = Color.Yellow,
                            size = Size(
                                faceImage.boundaries.width().dp.toPx(),
                                faceImage.boundaries.height().dp.toPx()
                            ),
                            topLeft = Offset(
                                scaledX,
                                scaledY
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

