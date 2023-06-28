package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.ImageClassifierService
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.FacialExpressionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import com.ionutv.livelinesdetection.utils.Constants
import com.ionutv.livelinesdetection.utils.elementPairs
import com.ionutv.livelinesdetection.utils.emptyString
import com.ionutv.livelinesdetection.utils.popOrNull
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class RandomFacialExpression(
    private val emotionClassifier: FacialExpressionImageClassifier,
    private val faceRecognition: FaceNetFaceRecognition,
    private val shouldCheckFaceSimilarity: Boolean = true,
    emotionsNumberToDetect: Int = 2
) : VerificationFlow {


    private val emotionsNumber: Int

    init {
        emotionsNumber = emotionsNumberToDetect.coerceIn(1..emotionClassifier.labels.size)
    }

    private sealed class State {
        object Start : State()
        object Detecting : State()
        object CheckAreAllImagesSame : State()
        object Finished : State()
        object Error : State()
    }

    private sealed class Event {
        object Start : Event()
        object Detected : Event()
        object FinishedEmotions : Event()
        object Error : Event()
    }

    private val _faceList = mutableListOf<Bitmap>()
    override val faceList: List<Bitmap> get() = _faceList.toList()

    private val emotionsToDetect: MutableSet<String> = mutableSetOf<String>().apply {
        val emotionLabels = emotionClassifier.labels.toMutableList()
        while (this.size != emotionsNumberToDetect) {
            val emotion = emotionLabels.random()
            if(emotion!="Disgusted" && emotion!="Sad")
                add(emotion)
            emotionLabels.remove(emotion)
        }
    }

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationStateFlow: StateFlow<VerificationState> =
        _verificationStateFlow.asStateFlow()

    private var emotionToDetect: String? = emptyString()

    private val machine = StateMachine.create<State, Event, Nothing> {
        initialState(State.Start)
        state<State.Start> {
            on<Event.Start> {
                transitionTo(State.Detecting)
            }
        }
        state<State.Detecting> {
            onEnter {
                emotionToDetect = emotionsToDetect.popOrNull()
                Log.d("RandomEmotion TEST", "Detecting")
                emotionToDetect?.let { emotion ->
                    _verificationStateFlow.update {
                        VerificationState.Working("Please try to be $emotion")
                    }
                }
            }
            on<Event.Detected> {
                transitionTo(State.Detecting)
            }
            on<Event.FinishedEmotions> {
                if(shouldCheckFaceSimilarity)
                {
                    transitionTo(State.CheckAreAllImagesSame)
                } else transitionTo(State.Finished)
            }

        }
        state<State.CheckAreAllImagesSame> {
            onEnter {
                Log.d("RandomEmotion TEST", "Checking face similarity")
                _verificationStateFlow.update {
                    VerificationState.Working("Please wait for additional checks")
                }
            }
            on<Event.Detected> {
                transitionTo(State.Finished)
            }
            on<Event.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Finished> {
            onEnter {
                Log.d("RandomEmotion TEST", "Detected")
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
            }
            on<Event.Start> {
                transitionTo(State.Detecting)
            }
        }
        state<State.Error> {
            onEnter {
                Log.d("RandomEmotions TEST", "Error different persons")
                _verificationStateFlow.update {
                    VerificationState.Error("Different person in at least one of the images")
                }
            }
            on<Event.Start> {
                transitionTo(State.Detecting)
            }
        }

    }

    override fun initialise() {
                _verificationStateFlow.update {
            VerificationState.Start
        }
        _faceList.clear()
        machine.transition(Event.Start)
    }

    override suspend fun performFaceCheck(face: FaceDetected) {
        when(machine.state){
            State.Detecting -> {
                if (emotionToDetect == null) {
                    machine.transition(Event.FinishedEmotions)
                } else {
                    val croppedBitmap =
                        ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
                    val result = emotionClassifier.classifyEmotions(croppedBitmap).first()
                    if (result.title == emotionToDetect && result.confidence > Constants.EMOTION_CONFIDENCE_THRESHOLD) {
                        machine.transition(Event.Detected)
                        _faceList.add(croppedBitmap)
                    }
                }
            }
            State.CheckAreAllImagesSame -> {
                if (checkAreAllImagesSamePerson()) {
                    machine.transition(Event.Detected)
                } else {
                    machine.transition(Event.Error)
                }
            }
            State.Error ->{
                //TODO
            }
            else -> {
                //TODO
            }
        }

    }

    private fun checkAreAllImagesSamePerson(): Boolean {
        val processedImages = faceList.map {
            faceRecognition.getImageProcessing(it)
        }
        val isDifferentPersonInList = elementPairs(processedImages).map {
            FaceNetFaceRecognition.isImageSamePerson(FaceNetFaceRecognition.MetricUsed.L2, it)
        }.any {
            !it
        }
        return !isDifferentPersonInList
    }
}