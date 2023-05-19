package com.ionutv.livelinesdetection.features.emotion_detection

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.ionutv.livelinesdetection.features.ImageClassifierService
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

internal class EmotionImageClassifier(application: Application) :
    ImageClassifierService(
        application,
        "emotion_model.tflite",
        "emotion_label.txt",
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

    class StandardizeOp : TensorOperator {

        override fun apply(p0: TensorBuffer?): TensorBuffer {
            val pixels = p0!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt( pixels.map{ pi -> ( pi - mean ).pow( 2 ) }.sum() / pixels.size.toFloat() )
            std = max( std , 1f / sqrt( pixels.size.toFloat() ))
            for ( i in pixels.indices ) {
                pixels[ i ] = ( pixels[ i ] - mean ) / std
            }
            val output = TensorBufferFloat.createFixedSize( p0.shape , DataType.FLOAT32 )
            output.loadArray( pixels )
            return output
        }

    }
    companion object{
        internal fun cropBitmapExample(
            bitmap: Bitmap,
            rect: Rect
        ): Bitmap {
            val ret = Bitmap.createBitmap(
                (rect.width() * 1.2f).toInt(),
                (rect.height() * 1.2f).toInt(),
                bitmap.config
            )
            val canvas = Canvas(ret)
            canvas.drawBitmap(bitmap, -rect.left * 0.90.toFloat(), -rect.top * 0.90.toFloat(), null)
            return ret
        }
    }

}