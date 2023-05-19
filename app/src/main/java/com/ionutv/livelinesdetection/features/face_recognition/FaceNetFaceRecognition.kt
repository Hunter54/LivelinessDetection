package com.ionutv.livelinesdetection.features.face_recognition

import android.app.Application
import android.graphics.Bitmap
import com.ionutv.livelinesdetection.features.ClassifierResult
import com.ionutv.livelinesdetection.features.ImageClassifierService
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

internal class FaceNetFaceRecognition(application: Application) : ImageClassifierService(
    application,
    "facenet.tflite",
    emptyString()
) {

    private val faceList = mutableListOf<FaceRecognitionResult>()

    fun processImage(bitmap: Bitmap): List<ClassifierResult> {
        inputImageBuffer = preProcessAndLoadImage(bitmap)

        val faceNetOutput =
            tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
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

    private fun computeL2Norm(x1: FloatArray, x2: FloatArray): Float {
        return sqrt(x1.mapIndexed { i, xi -> (xi - x2[i]).pow(2) }.sum())
    }

    private fun computeCosineSimilarity(first: FloatArray, second: FloatArray): Float {
        val mag1 = sqrt(first.map { it * it }.sum())
        val mag2 = sqrt(second.map { it * it }.sum())
        val dot = first.mapIndexed { i, j -> j * second[i] }.sum()
        return dot / (mag1 * mag2)
    }

    private class StandardizeOp : TensorOperator {
        override fun apply(p0: TensorBuffer): TensorBuffer {
            val pixels = p0.floatArray
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

    data class FaceRecognitionResult(
        val name: String,
        val embedding: FloatArray
    )

}