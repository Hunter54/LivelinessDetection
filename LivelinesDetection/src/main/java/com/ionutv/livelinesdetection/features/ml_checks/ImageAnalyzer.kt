package com.ionutv.livelinesdetection.features.ml_checks

import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.EnumSet

internal open class ImageAnalyzer(
    private val application: Application,
    private val viewModelScope: CoroutineScope
) {

    private val _detectionOption = MutableStateFlow(LivelinessDetectionOption.SMILE)
    internal val detectionOption = _detectionOption.asStateFlow()
    private val _faceDetectedResult = MutableSharedFlow<FaceDetected>()

    private val _resultFlow = MutableSharedFlow<FaceClassifierResult>()
    val resultFlow = _resultFlow.asSharedFlow()
    private val verificationFlow = _detectionOption.map { detectionOption ->
        when (detectionOption) {
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
        }.also {
            it.initialise()
        }
    }.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val verificationState = verificationFlow.combine(_faceDetectedResult) { flow, face ->
        flow.performFaceCheck(face)
        flow
    }.distinctUntilChanged().flatMapLatest {
        it.verificationStateFlow
    }

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

    fun resetFlow() {
        verificationFlow.replayCache.firstOrNull()?.initialise()
    }

    fun closeResources() {
        emotionClassifier.closeResource()
        faceNetFaceRecognition.closeResource()
    }

    fun updateDetectionOption(newDetectionOption: LivelinessDetectionOption) {
        _detectionOption.value = newDetectionOption
    }

    @ExperimentalGetImage
    fun analyzeImage(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }
        isProcessing = true
        val detectSmilingOrEyesOpen = _detectionOption.value in optionsWithMlKitClassification
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
        _faceDetectedResult.emit(it)
    }

}