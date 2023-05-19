package com.ionutv.livelinesdetection.features.emotion_detection

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.ionutv.livelinesdetection.features.ClassifierResult
import com.ionutv.livelinesdetection.features.ImageClassifierService
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class EmotionImageClassifier(
    application: Application,
    private val maxResultNumber: Int = 3
) : ImageClassifierService(
    application,
    "emotion_model.tflite",
    "emotion_label.txt",
) {
    private val preprocessNormalizeOp = NormalizeOp(0f, 255f)
    private val postProcessorNormalize0p = NormalizeOp(0.0f, 1.0f)
    private val probabilityProcessor =
        TensorProcessor.Builder().add(postProcessorNormalize0p).build()

    override fun processImage(bitmap: Bitmap): TensorBuffer {
        inputImageBuffer = preProcessAndLoadImage(bitmap)

        tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        return outputProbabilityBuffer
    }

    fun classifyEmotions(bitmap: Bitmap): List<ClassifierResult> =
        getProbabilityFromOutput(processImage(bitmap))

    fun getProbabilityFromOutput(probabilityBuffer: TensorBuffer): List<ClassifierResult> {
        val labeledProbability = TensorLabel(
            labels,
            probabilityProcessor.process(probabilityBuffer)
        ).mapWithFloatValue
        return getTopProbability(labeledProbability, maxResultNumber)
    }

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

    private fun getTopProbability(
        labelProb: Map<String, Float?>,
        numberOfProbabilities: Int = maxResultNumber,
    ): List<ClassifierResult> {
        // Find the best classifications.
        val list = buildList {
            labelProb.forEach {
                it.value?.let { value ->
                    add(
                        ClassifierResult(
                            it.key, it.key,
                            (value * 10000).roundToInt() / 100f, null
                        )
                    )
                }
            }
        }.sortedByDescending { it.confidence }

        val listSizeOrMaxSize = list.size.coerceAtMost(numberOfProbabilities)
        return list.take(listSizeOrMaxSize)
    }

    class StandardizeOp : TensorOperator {

        override fun apply(p0: TensorBuffer?): TensorBuffer {
            val pixels = p0!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt(pixels.map { pi -> (pi - mean).pow(2) }.sum() / pixels.size.toFloat())
            std = max(std, 1f / sqrt(pixels.size.toFloat()))
            for (i in pixels.indices) {
                pixels[i] = (pixels[i] - mean) / std
            }
            val output = TensorBufferFloat.createFixedSize(p0.shape, DataType.FLOAT32)
            output.loadArray(pixels)
            return output
        }

    }

    companion object {
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