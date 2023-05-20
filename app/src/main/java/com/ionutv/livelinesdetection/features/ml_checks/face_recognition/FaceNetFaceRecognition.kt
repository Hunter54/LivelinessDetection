package com.ionutv.livelinesdetection.features.ml_checks.face_recognition

import android.content.Context
import android.graphics.Bitmap
import com.ionutv.livelinesdetection.features.ml_checks.ImageClassifierService
import com.ionutv.livelinesdetection.utils.emptyString
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

internal class FaceNetFaceRecognition(context: Context) : ImageClassifierService(
    context,
    "facenet.tflite",
    emptyString()
) {

    private val faceList = mutableListOf<FaceRecognitionResult>()

    override fun processImage(bitmap: Bitmap): TensorBuffer {
        inputImageBuffer = preProcessAndLoadImage(bitmap)

        tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        return outputProbabilityBuffer
    }

    override fun preProcessAndLoadImage(bitmap: Bitmap): TensorImage {
        inputImageBuffer.load(bitmap)
        val cropSize = bitmap.width.coerceAtMost(bitmap.height)
        return ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.BILINEAR))
            .add(StandardizeOp())
            .build().process(inputImageBuffer)
    }

    fun computeL2Normalisation(first: FloatArray, second: FloatArray): Float {
        return sqrt(first.mapIndexed { i, j -> (j - second[i]).pow(2) }.sum())
    }

    fun computeCosineSimilarity(first: FloatArray, second: FloatArray): Float {
        val mag1 = sqrt(first.map { it * it }.sum())
        val mag2 = sqrt(second.map { it * it }.sum())
        val dot = first.mapIndexed { i, j -> j * second[i] }.sum()
        return dot / (mag1 * mag2)
    }

    private class StandardizeOp : TensorOperator {
        override fun apply(image: TensorBuffer): TensorBuffer {
            val pixels = image.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt(pixels.map { pixel -> (pixel - mean).pow(2) }.sum() / pixels.size.toFloat())
            std = max(std, 1f / sqrt(pixels.size.toFloat()))
            for (i in pixels.indices) {
                pixels[i] = (pixels[i] - mean) / std
            }
            val output = TensorBufferFloat.createFixedSize(image.shape, DataType.FLOAT32)
            output.loadArray(pixels)
            return output
        }

    }

    data class FaceRecognitionResult(
        val name: String,
        val embedding: FloatArray
    )

}