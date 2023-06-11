package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.ImageClassifierService
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.utils.emptyString
import com.ionutv.livelinesdetection.utils.popOrNull
import com.tinder.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class RandomEmotion(
    private val emotionClassifier: EmotionImageClassifier,
    private val emotionsNumberToDetect: Int = 2
) : VerificationFlow {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private sealed class State {
        object Start : State()
        object Detecting : State()
        object Detected : State()
    }

    private sealed class SideEffect {
        object Detecting : SideEffect()
        object Detected : SideEffect()
    }

    private sealed class Event {
        object Start : Event()
        object Detected : Event()
        object Finished : Event()
    }

    private val _faceList = mutableListOf<Bitmap>()
    override val faceList: List<Bitmap> get() = _faceList.toList()

    private val emotionsToDetect: MutableSet<String> = mutableSetOf<String>().apply {
        val emotionLabels = emotionClassifier.labels.toMutableList()
        while (this.size != emotionsNumberToDetect) {
            val emotion = emotionLabels.random()
            add(emotion)
            emotionLabels.remove(emotion)
        }
    }

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationFlowState: StateFlow<VerificationState> =
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
                _verificationStateFlow.update {
                    VerificationState.Working("Please try to be $emotionToDetect")
                }
            }
            on<Event.Detected> {
                transitionTo(State.Detecting)
            }
            on<Event.Finished> {
                transitionTo(State.Detected)
            }

        }
        state<State.Detected> {
            onEnter {
                Log.d("RandomEmotion TEST", "Detected")
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
            }
        }

    }

    override fun initialise() {
        machine.transition(Event.Start)

    }

    override suspend fun invokeVerificationFlow(face: FaceDetected) {
        val croppedBitmap =
            ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
        if (emotionToDetect == null) {
            machine.transition(Event.Finished)
        } else {
            val result = emotionClassifier.classifyEmotions(croppedBitmap).first()
            if (result.title == emotionToDetect && result.confidence > 0.5f) {
                machine.transition(Event.Detected)
            }
        }
    }
}