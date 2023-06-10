package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.tinder.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class AngledFacesWithSmile : VerificationFlow {

    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val smileFlow = Smile()
    private val angledFacesFlow = AngledFaces()

    private sealed class State() {
        object Start : State()
        object DetectSmile : State()
        object DetectAngledFaces : State()
        object Finished : State()
    }

    private sealed class Event {
        object Start : Event()
        object DetectSmile : Event()
        object DetectAngledFaces : Event()
        object Finish : Event()
        object Reset : Event()
    }

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationFlowState: StateFlow<VerificationState> =
        _verificationStateFlow.asStateFlow()


    private val machine = StateMachine.create<State, Event, Nothing> {
        initialState(State.Start)
        state<State.Start> {
            on<Event.DetectSmile> {
                transitionTo(State.DetectSmile)
            }
            on<Event.DetectAngledFaces> {
                transitionTo(State.DetectAngledFaces)
            }
        }
        state<State.DetectAngledFaces> {
            on<Event.DetectSmile> {
                transitionTo(State.DetectSmile)
            }
            on<Event.Finish> {
                transitionTo(State.Finished)
            }
            onEnter {
                Log.d("Angled and smile TEST", "Detecting Angled Faces")
            }
        }
        state<State.DetectSmile> {
            on<Event.DetectAngledFaces> {
                transitionTo(State.DetectAngledFaces)
            }
            on<Event.Finish> {
                transitionTo(State.Finished)
            }
            onEnter {
                Log.d("Angled and smile TEST", "Detecting Smiles")

            }
        }
        state<State.Finished> {
            onEnter {
                Log.d("Angled and smile TEST", "Finished")
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
            }
        }
    }

    init {
        val shouldStartWithSmile = Random.nextBoolean()
        if (shouldStartWithSmile) {
            machine.transition(Event.DetectSmile)
        } else {
            machine.transition(Event.DetectAngledFaces)
        }
        merge(smileFlow.verificationFlowState, angledFacesFlow.verificationFlowState).filter {
            it is VerificationState.Working || it is VerificationState.Error
        }.onEach {verificationState ->
            _verificationStateFlow.update {
                verificationState
            }
        }.launchIn(coroutineScope)
    }

    override suspend fun invokeVerificationFlow(face: FaceDetected) {
        if (smileFlow.verificationFlowState.value == VerificationState.Finished && angledFacesFlow.verificationFlowState.value == VerificationState.Finished) {
            machine.transition(Event.Finish)
            return
        }
        when (machine.state) {
            State.DetectAngledFaces -> {
                angledFacesFlow.invokeVerificationFlow(face)
                if (angledFacesFlow.verificationFlowState.value == VerificationState.Finished)
                    machine.transition(Event.DetectSmile)
            }

            State.DetectSmile -> {
                smileFlow.invokeVerificationFlow(face)
                if (smileFlow.verificationFlowState.value == VerificationState.Finished)
                    machine.transition(Event.DetectAngledFaces)
            }

            State.Finished -> {
                coroutineScope.cancel()
            }

            State.Start -> {

            }
        }
    }
}