package com.ionutv.livelinesdetection.features

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
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.MappedByteBuffer
import kotlin.math.roundToInt

public data class ClassifierResult(
    val id: String,
    val title: String,
    val confidence: Float,
    var location: RectF?
)

internal abstract class ImageClassifierService constructor(
    context: Context,
    modelPath: String,
    labelPath: String,
    protected val preprocessNormalizeOp: TensorOperator,
    postProcessorNormalize0p: TensorOperator,
    private val maxResultNumber: Int = 3,
) {
    companion object {
        private const val LOG_TAG = "TENSERFLOW_EMOTION_LITE_TAG"
    }

    private val tfliteModel: MappedByteBuffer = FileUtil.loadMappedFile(context, modelPath)

    private val labels: List<String> = FileUtil.loadLabels(context, labelPath)

    protected var imageSizeX: Int

    protected var imageSizeY: Int

    protected var inputImageBuffer: TensorImage

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

    abstract fun preProcessAndLoadImage(bitmap: Bitmap): TensorImage

    fun processImage(bitmap: Bitmap): List<ClassifierResult> {
        inputImageBuffer = preProcessAndLoadImage(bitmap)

        tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        val labeledProbability = TensorLabel(
            labels,
            probabilityProcessor.process(outputProbabilityBuffer)
        ).mapWithFloatValue
        return getTopProbability(labeledProbability)
    }

    fun closeResource(){
        tflite.close()
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
}