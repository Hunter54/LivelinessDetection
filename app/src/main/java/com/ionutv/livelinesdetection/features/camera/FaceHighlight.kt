package com.ionutv.livelinesdetection.features.camera

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ionutv.livelinesdetection.features.face_detection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.face_detection.FaceDetected

@Composable
internal fun CameraPreviewAndFaceHighlight(
    cameraSelector: CameraSelector,
    cameraViewModel: CameraViewModel,
    faceImage: FaceAnalyzerResult,
    modifier: Modifier = Modifier,
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
                    when (faceImage) {
                        is FaceDetected -> {

                            val faceHighlight =
                                FaceHighlight(faceImage, maxWidth.toPx(), maxHeight.toPx())

                            faceHighlight.drawFaceHighlight()

                        }

                        is FaceAnalyzerResult.Error -> {

                        }

                        FaceAnalyzerResult.MultipleFaceInsideFrame -> {

                        }

                        FaceAnalyzerResult.NoFaceDetected -> {

                        }
                    }

                }
            }
        }
    }
}

internal class FaceHighlight(
    private val detectedFace: FaceDetected,
    private val screenWidth: Float,
    private val screenHeight: Float
) {

    val viewAspectRatio = screenWidth / screenHeight
    val imageAspectRatio =
        detectedFace.imageWidth.toFloat() / detectedFace.imageHeight.toFloat()
    val scaleFactor: Float
    val postScaleHeightOffset: Float
    val postScaleWidthOffset: Float
    val x: Float
    val y: Float
    val left: Float
    val top: Float
    val right: Float
    val bottom: Float


    init {
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = screenWidth / detectedFace.imageWidth
            postScaleHeightOffset =
                (screenWidth / imageAspectRatio - screenHeight) / 2
            postScaleWidthOffset = 0.0f
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = screenHeight / detectedFace.imageHeight
            postScaleWidthOffset =
                (screenHeight * imageAspectRatio - screenWidth) / 2
            postScaleHeightOffset = 0.0f
        }
        // Translate X and Y coordinates to match image scaled for screen
        x = translateX(detectedFace.boundaries.centerX().toFloat())
        y = translateY(detectedFace.boundaries.centerY().toFloat())

        left = x - scale(detectedFace.boundaries.width() / 2.0f)
        top = y - scale(detectedFace.boundaries.height() / 2.0f)
        right = x + scale(detectedFace.boundaries.width() / 2.0f)
        bottom = y + scale(detectedFace.boundaries.height() / 2.0f)
    }

    private fun translateX(x: Float) = screenWidth - (scale(x) - postScaleWidthOffset)
    private fun translateY(y: Float) = scale(y) - postScaleHeightOffset
    private fun scale(imagePixel: Float): Float = imagePixel * scaleFactor

    context(DrawScope)
    fun drawFaceHighlight() {
        drawRect(
            color = Color.White,
            size = Size(
                (left - right),
                (bottom - top)
            ),
            //Image is flipped to x needs to be right not left
            topLeft = Offset(
                right,
                top
            ),
            style = Stroke(width = 2.dp.toPx())
        )

    }

}

//class FaceHighlightScaling(){
//    fun calculate(){
//        val maxWidthPx = maxWidth.toPx()
//        val maxHeightPx = maxHeight.toPx()
//        val viewAspectRatio = maxWidthPx / maxHeightPx
//        val imageAspectRatio = faceImage.imageWidth.toFloat() / faceImage.imageHeight.toFloat()
//        val scaleFactor: Float
//        var postScaleHeightOffset: Float = 0f
//        var postScaleWidthOffset: Float = 0f
//
//        if (viewAspectRatio > imageAspectRatio) {
//            // The image needs to be vertically cropped to be displayed in this view.
//            scaleFactor = maxWidthPx / faceImage.imageWidth
//            postScaleHeightOffset =
//                (maxWidthPx / imageAspectRatio - maxHeightPx) / 2
//        } else {
//            // The image needs to be horizontally cropped to be displayed in this view.
//            scaleFactor = maxHeightPx / faceImage.imageHeight
//            postScaleWidthOffset =
//                (maxHeightPx * imageAspectRatio - maxWidthPx) / 2
//        }
//        // Translate X and Y coordinates to match image scaled for screen
//        val scaledX = maxWidthPx - (faceImage.boundaries.left * scaleFactor + postScaleWidthOffset)
//        val scaledY = faceImage.boundaries.top * scaleFactor - postScaleHeightOffset
//        drawRect(
//            color = Color.Yellow,
//            size = Size(
//                faceImage.boundaries.width().dp.toPx() / scaleFactor,
//                faceImage.boundaries.height().dp.toPx()/ scaleFactor
//            ),
//            topLeft = Offset(
//                scaledX,
//                scaledY
//            ),
//            style = Stroke(width = 6.dp.toPx())
//        )
//    }
//}