package com.ionutv.livelinesdetection.features.ml_checks

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.AngledFaces
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.AngledFacesWithRandomEmotion
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.AngledFacesWithSmile
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.RandomEmotion
import com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows.Smile
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetectionResult
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.detectFace
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.EnumSet

internal open class ImageAnalyzerCommon(
    private val application: Application,
    private val viewModelScope: CoroutineScope,
    private var detectionOption: LivelinessDetectionOption,
    private val debugMode: Boolean
) {

    private val emotionClassifier: EmotionImageClassifier = EmotionImageClassifier(application)
    private val faceNetFaceRecognition: FaceNetFaceRecognition = FaceNetFaceRecognition(application)
    private val optionsWithMlKitClassification: Set<LivelinessDetectionOption> = EnumSet.of(
        LivelinessDetectionOption.SMILE,
//        LivelinessDetectionOption.BLINK,
//        LivelinessDetectionOption.SMILE_BLINK,
//        LivelinessDetectionOption.RANDOM_EMOTION_BLINK,
        LivelinessDetectionOption.ANGLED_FACES_WITH_SMILE,
//        LivelinessDetectionOption.ANGLED_FACES_WITH_EMOTION_BLINK
    )

    private var isProcessing = false
    private val metricToBeUsed = FaceNetFaceRecognition.MetricUsed.L2
    private val nameScoreHashmap = HashMap<String, ArrayList<Float>>()


    private var verificationFlow = when (detectionOption) {
        LivelinessDetectionOption.SMILE -> Smile()
        LivelinessDetectionOption.RANDOM_EMOTION -> RandomEmotion(
            emotionClassifier,
            faceNetFaceRecognition
        )

        LivelinessDetectionOption.ANGLED_FACES -> AngledFaces(faceNetFaceRecognition)
        LivelinessDetectionOption.ANGLED_FACES_WITH_SMILE -> AngledFacesWithSmile(
            faceNetFaceRecognition
        )

        LivelinessDetectionOption.ANGLED_FACES_WITH_EMOTION -> AngledFacesWithRandomEmotion(
            emotionClassifier,
            faceNetFaceRecognition
        )
    }
    internal var verificationState = verificationFlow.verificationStateFlow
        private set

    internal fun changeDetectionOption(detectionOption: LivelinessDetectionOption) {
        this.verificationFlow = when (detectionOption) {
            LivelinessDetectionOption.SMILE -> Smile()
            LivelinessDetectionOption.RANDOM_EMOTION -> RandomEmotion(
                emotionClassifier,
                faceNetFaceRecognition
            )

            LivelinessDetectionOption.ANGLED_FACES -> AngledFaces(faceNetFaceRecognition)
            LivelinessDetectionOption.ANGLED_FACES_WITH_SMILE -> AngledFacesWithSmile(
                faceNetFaceRecognition
            )

            LivelinessDetectionOption.ANGLED_FACES_WITH_EMOTION -> AngledFacesWithRandomEmotion(
                emotionClassifier,
                faceNetFaceRecognition
            )
        }
        this.detectionOption = detectionOption
        this.verificationState = verificationFlow.verificationStateFlow
        this.verificationFlow.initialise()
    }

    private var lastAnalyzedFace = FaceClassifierResult.NoFaceDetected
    protected val _resultFlow = MutableSharedFlow<FaceClassifierResult>()
    val resultFlow = _resultFlow.asSharedFlow()

    var faceList = mutableListOf<FaceNetFaceRecognition.FaceRecognitionResult>()

    init {
        verificationFlow.initialise()
    }

    fun addImageToFaceList(bitmap: Bitmap, name: String) {
        val p = ArrayList<Float>()

        val result = faceNetFaceRecognition.processImage(bitmap)
        faceList.add(FaceNetFaceRecognition.FaceRecognitionResult(name, result.floatArray))
    }

    fun resetFlow() {
        verificationFlow.initialise()
    }

    fun closeResources() {
        emotionClassifier.closeResource()
        faceNetFaceRecognition.closeResource()
    }

    @ExperimentalGetImage
    fun analyzeImage(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        isProcessing = true
        val detectSmilingOrEyesOpen = detectionOption in optionsWithMlKitClassification
        detectFace(image, detectSmilingOrEyesOpen) {
            viewModelScope.launch(Dispatchers.IO) {
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

    private suspend fun analyzeFace(it: FaceDetected) {
        if (!debugMode) {
            verificationFlow.invokeVerificationFlow(it)
        } else {
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
    }

    private fun checkFaceSimilarity(embedding: TensorBuffer): String {
        val subject = embedding.floatArray
        faceList.forEach { face ->
            if (nameScoreHashmap[face.name] == null) {
                // Compute the L2 norm and then append it to the ArrayList.
                val p = ArrayList<Float>()
                when (metricToBeUsed) {
                    FaceNetFaceRecognition.MetricUsed.COSINE -> {
                        p.add(
                            FaceNetFaceRecognition.computeCosineSimilarity(
                                subject,
                                face.embedding
                            )
                        )
                    }

                    FaceNetFaceRecognition.MetricUsed.L2 -> {
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
                    FaceNetFaceRecognition.MetricUsed.COSINE -> {
                        nameScoreHashmap[face.name]?.add(
                            FaceNetFaceRecognition.computeCosineSimilarity(subject, face.embedding)
                        )
                    }

                    FaceNetFaceRecognition.MetricUsed.L2 -> {
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
            FaceNetFaceRecognition.MetricUsed.COSINE -> {
                if (avgScores.maxOrNull()!! > 0.4f) {
                    names[avgScores.indexOf(avgScores.maxOrNull()!!)]
                } else {
                    "Unknown"
                }
            }

            FaceNetFaceRecognition.MetricUsed.L2 -> {
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