package com.ionutv.livelinesdetection.features.ml_checks

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.ionutv.livelinesdetection.features.camera.LivelinessDetectionOption
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetectionResult
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.detectFace
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

internal open class ImageAnalyzerCommon(
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private val detectionOption: LivelinessDetectionOption
) : ImageAnalysis.Analyzer {

    private val emotionClassifier: EmotionImageClassifier = EmotionImageClassifier(application)
    private val faceNetFaceRecognition: FaceNetFaceRecognition = FaceNetFaceRecognition(application)
    private var isProcessing = false
    private val metricToBeUsed = MetricUsed.L2
    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()

    protected val _resultFlow = MutableSharedFlow<FaceClassifierResult>()
    val resultFlow = _resultFlow.asSharedFlow()

    var faceList = mutableListOf<FaceNetFaceRecognition.FaceRecognitionResult>()

    fun addImageToFaceList(bitmap: Bitmap, name: String) {
        val p = ArrayList<Float>()

        val result = faceNetFaceRecognition.processImage(bitmap)
        faceList.add(FaceNetFaceRecognition.FaceRecognitionResult(name, result.floatArray))
    }

    fun closeResources() {
        emotionClassifier.closeResource()
        faceNetFaceRecognition.closeResource()
    }

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        isProcessing = true
        detectFace(image) {
            viewModelScope.launch {
                when (it) {
                    is FaceDetectionResult.Error -> {
                        _resultFlow.emit(FaceClassifierResult.Error(it.error))
                    }

                    is FaceDetected -> {
                        analyzeFace(it)
                    }

                    FaceDetectionResult.MultipleFaceInsideFrame -> {
                        _resultFlow.emit(FaceClassifierResult.Error("Multiple faces in frame"))
                    }

                    FaceDetectionResult.NoFaceDetected -> {
                        _resultFlow.emit(FaceClassifierResult.NoFaceDetected)
                    }
                }
                isProcessing = false
            }
        }
    }

    private enum class MetricUsed {
        COSINE,
        L2
    }

    private suspend fun analyzeFace(it: FaceDetected) {
//        when(detectionOption){
//            LivelinessDetectionOption.SMILE -> TODO()
//            LivelinessDetectionOption.BLINK -> TODO()
//            LivelinessDetectionOption.RANDOM_EMOTION -> TODO()
//            LivelinessDetectionOption.SMILE_BLINK -> TODO()
//            LivelinessDetectionOption.RANDOM_EMOTION_BLINK -> {
//
//            }
//            LivelinessDetectionOption.ANGLED_FACES_WITH_EMOTION -> TODO()
//            LivelinessDetectionOption.ANGLED_FACES_WITH_EMOTION_BLINK -> TODO()
//        }
        val croppedBitmap =
            ImageClassifierService.cropBitmapExample(it.image, it.boundaries)
        val result = emotionClassifier.classifyEmotions(croppedBitmap)
        if (result.isNotEmpty()) {
            Log.d("EMOTION_CLASSIFIER IS", result.first().toString())
        }
        val embedding = faceNetFaceRecognition.processImage(croppedBitmap)
        val userName = checkFaceSimilarity(embedding)
        _resultFlow.emit(
            FaceClassifierResult.FaceClassified(
                it.boundaries,
                image = it.image,
                croppedImage = croppedBitmap,
                emotions = result,
                name = userName,
                faceAngle = it.headAngle
            )
        )
    }

    private fun checkFaceSimilarity(embedding: TensorBuffer): String {
        val subject = embedding.floatArray
        faceList.forEach { face ->
            if (nameScoreHashmap[face.name] == null) {
                // Compute the L2 norm and then append it to the ArrayList.
                val p = ArrayList<Float>()
                when (metricToBeUsed) {
                    MetricUsed.COSINE -> {
                        p.add(
                            FaceNetFaceRecognition.computeCosineSimilarity(
                                subject,
                                face.embedding
                            )
                        )
                    }

                    MetricUsed.L2 -> {
                        p.add(
                            FaceNetFaceRecognition.computeL2Normalisation(
                                subject,
                                face.embedding
                            )
                        )
                    }
                }
                nameScoreHashmap[face.name] = p
            }
            // If this cluster exists, append the L2 norm/cosine score to it.
            else {
                when (metricToBeUsed) {
                    MetricUsed.COSINE -> {
                        nameScoreHashmap[face.name]?.add(
                            FaceNetFaceRecognition.computeCosineSimilarity(subject, face.embedding)
                        )
                    }

                    MetricUsed.L2 -> {
                        nameScoreHashmap[face.name]?.add(
                            FaceNetFaceRecognition.computeL2Normalisation(subject, face.embedding)
                        )
                    }
                }
            }
        }
        val avgScores = nameScoreHashmap.values.map { scores ->
            scores.toFloatArray().average()
        }
        Log.d("FACE_RECOGNITION", "Average score for each user : $nameScoreHashmap")

        val names = nameScoreHashmap.keys.toTypedArray()
        nameScoreHashmap.clear()

        val bestScoreName = when (metricToBeUsed) {
            MetricUsed.COSINE -> {
                if (avgScores.maxOrNull()!! > 0.4f) {
                    names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                } else {
                    "Unknown"
                }
            }

            MetricUsed.L2 -> {
                if (avgScores.minOrNull()!! > 10f) {
                    "Unknown"
                } else {
                    names[avgScores.indexOf(avgScores.minOrNull()!!)]
                }
            }
        }
        return bestScoreName
    }
}