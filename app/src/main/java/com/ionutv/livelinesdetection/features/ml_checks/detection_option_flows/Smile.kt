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

    private val machine = StateMachine.create<State,Event,Nothing> {
        initialState(State.Start)
        state<State.Start> {
            on<Event.Start> {
                _verificationStateFlow.update {
                    VerificationState.Working("Please try to smile")
                }
                transitionTo(State.Detecting)
            }
        }
        state<State.Detecting> {
            on<Event.Detected> {
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
                transitionTo(State.Detected)
            }
        }
        state<State.Detected> {
            onEnter {
                Log.d("LOGGING TEST", "Detected")
            }
        }
        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
        }
    }

    init {
        machine.transition(Event.Start)
    }

    override suspend fun invokeVerificationFlow(faceClassified: FaceDetected) {
        if (faceClassified.smiling == true) {
            machine.transition(Event.Detected)
            Log.d("LOGGING TEST", "SMILLING")
        }
        Log.d("LOGGING TEST", machine.state.toString())
    }

}