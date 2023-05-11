package com.ionutv.livelinesdetection.features.emotion_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer

internal class ClassifierService constructor(
    context: Context,
    modelPath: String,
    labelPath: String,
    private val preprocessNormalizeOp: TensorOperator,
    postProcessorNormalize0p: TensorOperator,
    private val maxResultNumber: Int = 3,
) {
    companion object {


        private const val LOG_TAG = "TENSERFLOW_LITE_TAG"
    }

    private val tfliteModel: MappedByteBuffer = FileUtil.loadMappedFile(context, modelPath)

    private val labels: MutableList<String> = FileUtil.loadLabels(context, labelPath)

    private var imageSizeX: Int

    private var imageSizeY: Int

    private var inputImageBuffer: TensorImage

    private val outputProbabilityBuffer: TensorBuffer

    private val probabilityProcessor: TensorProcessor

    private val tfliteOptions = Interpreter.Options().also {
        val compatList = CompatibilityList()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            val delegateOptions = compatList.bestOptionsForThisDevice
            it.addDelegate(GpuDelegate(delegateOptions))
        } else {
            it.numThreads = 4
        }
    }

    private val tflite: Interpreter = Interpreter(tfliteModel, tfliteOptions)

    data class Recognition(
        val id: String,
        val title: String,
        val confidence: Float,
        var location: RectF?
    )

    init {
        val imageTensorIndex = 0
        val imageShape = tflite.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
        imageSizeX = imageShape[1]
        imageSizeY = imageShape[2]

        val imageDataType = tflite.getInputTensor(imageTensorIndex).dataType()
        val probabilityTensorIndex = 0
        val probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape()
        val probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType()

        inputImageBuffer = TensorImage(imageDataType)
        outputProbabilityBuffer =
            TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
        probabilityProcessor = TensorProcessor.Builder().add(postProcessorNormalize0p).build()
        Log.d(LOG_TAG, "Created Tenserflow Lite Image Classifier")
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        inputImageBuffer.load(bitmap)
        val cropSize = bitmap.width.coerceAtMost(bitmap.height)

        return ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(TransformToGrayscaleOp())
            .add(preprocessNormalizeOp)
            .build().process(inputImageBuffer)
    }

    fun processImage(bitmap: Bitmap): List<Recognition> {
        inputImageBuffer = loadImage(bitmap)

        tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        val labeledProbability = TensorLabel(
            labels,
            probabilityProcessor.process(outputProbabilityBuffer)
        ).mapWithFloatValue
        return getTopProbability(labeledProbability)
    }

    private fun getTopProbability(
        labelProb: Map<String, Float?>,
        numberOfProbabilities: Int = maxResultNumber,
    ): List<Recognition> {
        // Find the best classifications.
        val list = buildList<Recognition> {
            labelProb.forEach {
                it.value?.let { it1 ->
                    add(
                        Recognition(
                            it.key, it.key,
                            it1, null
                        )
                    )
                }
            }
        }.sortedByDescending { it.confidence }

        val listSizeOrMaxSize = list.size.coerceAtMost(numberOfProbabilities)
        return list.take(listSizeOrMaxSize)
    }
}