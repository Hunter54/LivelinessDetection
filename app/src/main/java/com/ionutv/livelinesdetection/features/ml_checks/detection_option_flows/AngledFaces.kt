package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue

internal class AngledFaces : VerificationFlow {

    private sealed class State() {
        object Start : State()
        object DetectingFaceStraight : State()
        object DetectingLeftSide : State() {
            const val requiredFaceAngle: Int = 25
        }
        object DetectingRightSide : State() {
            const val requiredFaceAngle: Int = -25
        }
        object Detected : State()
    }

    private sealed class Event {
        object Start : Event()
        object Detected : Event()
        object Reset : Event()
    }

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationFlowState: StateFlow<VerificationState> =
        _verificationStateFlow.asStateFlow()

    private val machine = StateMachine.create<State, Event, Nothing> {
        initialState(State.Start)
        state<State.Start> {
            on<Event.Start> {
                transitionTo(State.DetectingFaceStraight)
            }
        }
        state<State.DetectingFaceStraight> {
            on<Event.Detected> {
                transitionTo(State.DetectingLeftSide)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.DetectingLeftSide> {
            on<Event.Detected> {
                transitionTo(State.DetectingRightSide)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.DetectingRightSide> {
            on<Event.Detected> {
                transitionTo(State.Detected)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.Detected> {
            onEnter {
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
        when (val state = machine.state) {
            is State.DetectingFaceStraight -> {
                if (face.headAngle.absoluteValue < 5) {
                    machine.transition(Event.Detected)
                }
            }

            is State.DetectingLeftSide -> {
                if (face.headAngle in (state.requiredFaceAngle - 5)..state.requiredFaceAngle + 5) {
                    machine.transition(Event.Detected)
                }
            }

            is State.DetectingRightSide -> {
                if (face.headAngle in (state.requiredFaceAngle - 5)..state.requiredFaceAngle + 5) {
                    machine.transition(Event.Detected)
                }
            }
            State.Start -> TODO()
            State.Detected -> TODO()

        }
    }
}