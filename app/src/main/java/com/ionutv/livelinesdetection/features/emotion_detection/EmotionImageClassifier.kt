package com.ionutv.livelinesdetection.features.emotion_detection

import android.app.Application
import android.graphics.Bitmap
import com.ionutv.livelinesdetection.features.ImageClassifierService
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp

internal class EmotionImageClassifier(application: Application, modelPath: String, labelPath: String) :
    ImageClassifierService(
        application,
        modelPath,
        labelPath,
        NormalizeOp(0f, 255f),
        NormalizeOp(0.0f, 1.0f)
    ) {
    override fun preProcessAndLoadImage(bitmap: Bitmap): TensorImage {
        inputImageBuffer.load(bitmap)
        val cropSize = bitmap.width.coerceAtMost(bitmap.height)
        return ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(TransformToGrayscaleOp())
            .add(preprocessNormalizeOp)
            .build().process(inputImageBuffer)
    }

}