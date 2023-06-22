package com.ionutv.livelinesdetection.features.ml_checks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

public data class ClassifierResult(
    val id: String,
    val title: String,
    val confidence: Float,
    var location: RectF?
)

internal abstract class ImageClassifierService constructor(
    context: Context,
    modelPath: String,
    labelPath: String
) {

    companion object {
        private const val LOG_TAG = "TENSERFLOW_EMOTION_LITE_TAG"
        fun cropBitmapExample(
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

    protected var imageSizeX: Int

    protected var imageSizeY: Int

    protected var inputImageBuffer: TensorImage

    protected val outputProbabilityBuffer: TensorBuffer

    private val tfliteOptions = Interpreter.Options().apply {
        val compatList = CompatibilityList()

        if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            val delegateOptions = compatList.bestOptionsForThisDevice
            addDelegate(GpuDelegate(delegateOptions))
        } else {
            numThreads = 4
        }
        useXNNPACK = true
        useNNAPI = true
    }

    //Retrieve labels only if path is not empty
    internal val labels: List<String> =
        labelPath.takeIf { it.isNotEmpty() }?.let { FileUtil.loadLabels(context, it) } ?: listOf()

    protected val tflite: Interpreter =
        Interpreter(FileUtil.loadMappedFile(context, modelPath), tfliteOptions)

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
        Log.d(LOG_TAG, "Created Tenserflow Lite Image Classifier")
    }

    abstract fun preProcessAndLoadImage(bitmap: Bitmap): TensorImage

    internal abstract fun processImage(bitmap: Bitmap): TensorBuffer

    fun closeResource() {
        tflite.close()
    }
}