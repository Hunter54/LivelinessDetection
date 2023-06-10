package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.ImageClassifierService
import com.ionutv.livelinesdetection.features.ml_checks.emotion_detection.EmotionImageClassifier
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class RandomEmotion(private val emotionClassifier: EmotionImageClassifier) :
    VerificationFlow {


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
    }

    private val emotionToDetect = emotionClassifier.labels.random()

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationFlowState: StateFlow<VerificationState> =
        _verificationStateFlow.asStateFlow()

    private val machine = StateMachine.create<State, Event, Nothing> {
        initialState(State.Start)
        state<State.Start> {
            on<Event.Start> {
                transitionTo(State.Detecting)
            }
        }
        state<State.Detecting> {
            onEnter {
                Log.d("RandomEmotion TEST", "Detecting")
                _verificationStateFlow.update {
                    VerificationState.Working("Please try to be $emotionToDetect")
                }
            }
            on<Event.Detected> {
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

    init {
        machine.transition(Event.Start)
    }

    override suspend fun invokeVerificationFlow(face: FaceDetected) {
        val croppedBitmap =
            ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
        val result = emotionClassifier.classifyEmotions(croppedBitmap).first()
        if (result.title == emotionToDetect && result.confidence > 0.5f) {
            machine.transition(Event.Detected)
        }
    }
}