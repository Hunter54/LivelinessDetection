package com.ionutv.livelinesdetection.features

import androidx.camera.core.UseCase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ionutv.livelinesdetection.features.camera.CameraPreview
import com.ionutv.livelinesdetection.features.facedetection.FaceAnalyzerResult
import com.ionutv.livelinesdetection.features.facedetection.FaceDetected

@Composable
fun CameraPreviewAndFaceHighlight(
    onPreviewUseCase: (UseCase) -> Unit,
    faceImage: FaceAnalyzerResult,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onUseCase = {
                onPreviewUseCase(it)
            })
            Canvas(modifier = modifier.fillMaxSize()) {
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

class FaceHighlight(
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
    fun drawFaceHighlight(){
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