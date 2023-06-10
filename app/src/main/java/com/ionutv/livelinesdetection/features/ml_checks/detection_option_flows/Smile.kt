package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


internal class Smile : VerificationFlow {

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
                Log.d("SMILE TEST", "Detecting smile")
                _verificationStateFlow.update {
                    VerificationState.Working("Please try to smile")
                }
            }
            on<Event.Detected> {
                transitionTo(State.Detected)
            }
        }
        state<State.Detected> {
            onEnter {
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
                Log.d("SMILE TEST", "Detected smile")
            }
        }
    }



    override suspend fun invokeVerificationFlow(faceClassified: FaceDetected) {
        machine.transition(Event.Start)
        if (faceClassified.smiling == true) {
            machine.transition(Event.Detected)
            Log.d("SMILE TEST", "SMILLING")
        }
    }

}