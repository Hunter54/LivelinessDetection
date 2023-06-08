package com.ionutv.livelinesdetection.features.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionutv.livelinesdetection.features.ml_checks.FaceClassifierResult

@Composable
internal fun CameraPreviewAndFaceHighlight(
    cameraSelector: CameraSelector,
    cameraProvider: ProcessCameraProvider?,
    imageAnalysisUseCase: ImageAnalysis,
    classifiedFace: FaceClassifierResult,
    modifier: Modifier = Modifier,
) {
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
                            imageAnalysisUseCase
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
                            drawImage(classifiedFace.croppedImage.asImageBitmap())
                            val faceHighlight =
                                FaceHighlight(classifiedFace, maxWidth.toPx(), maxHeight.toPx())
                            var emotionsString = ""
                            classifiedFace.emotions.forEach {
                                emotionsString += "${it.title} : ${it.confidence}\n"
                            }
                            faceHighlight.drawFaceHighlight(
                                textMeasurer,
                                emotionsString,
                                classifiedFace.name,
                                classifiedFace.faceAngle
                            )

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

internal class FaceHighlight(
    private val faceImage: FaceClassifierResult.FaceClassified,
    private val screenWidth: Float,
    private val screenHeight: Float
) {

    val viewAspectRatio = screenWidth / screenHeight
    val imageAspectRatio =
        faceImage.image.width.toFloat() / faceImage.image.height.toFloat()
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
            scaleFactor = screenWidth / faceImage.image.width
            postScaleHeightOffset =
                (screenWidth / imageAspectRatio - screenHeight) / 2
            postScaleWidthOffset = 0.0f
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = screenHeight / faceImage.image.height
            postScaleWidthOffset =
                (screenHeight * imageAspectRatio - screenWidth) / 2
            postScaleHeightOffset = 0.0f
        }
        // Translate X and Y coordinates to match image scaled for screen
        x = translateX(faceImage.boundaries.centerX().toFloat())
        y = translateY(faceImage.boundaries.centerY().toFloat())

        left = x - scale(faceImage.boundaries.width() / 2.0f)
        top = y - scale(faceImage.boundaries.height() / 2.0f)
        right = x + scale(faceImage.boundaries.width() / 2.0f)
        bottom = y + scale(faceImage.boundaries.height() / 2.0f)
    }

    private fun translateX(x: Float) = screenWidth - (scale(x) - postScaleWidthOffset)
    private fun translateY(y: Float) = scale(y) - postScaleHeightOffset
    private fun scale(imagePixel: Float): Float = imagePixel * scaleFactor

    context(DrawScope)
    @OptIn(ExperimentalTextApi::class)
    fun drawFaceHighlight(textMeasurer: TextMeasurer, emotions: String, name: String, faceAngle: Int) {
        val annotatedText = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            ) {
                if (name.isNotEmpty()) {
                    append(name + "\n")
                }
                append("Y angle: $faceAngle \n")
                append(emotions)
            }
        }
        val result = textMeasurer.measure(annotatedText)
        drawRect(
            color = Color.Gray,
            topLeft = Offset(left, top - result.size.height),
            size = Size(result.size.width.toFloat(), result.size.height.toFloat())
        )
        drawText(
            textMeasurer = textMeasurer,
            annotatedText,
            topLeft = Offset(left, top - result.size.height)
        )
        drawRect(
            color = Color.White,
            size = Size(
                (left - right),
                (bottom - top)
            ),
            //Image is flipped so x needs to be right not left
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