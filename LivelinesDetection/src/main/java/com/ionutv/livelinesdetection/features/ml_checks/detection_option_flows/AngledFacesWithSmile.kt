package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import com.ionutv.livelinesdetection.utils.elementPairs
import com.tinder.StateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

internal class AngledFacesWithSmile(private val faceRecognition: FaceNetFaceRecognition) :
    VerificationFlow {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var smileFlow : Smile
    private lateinit var angledFacesFlow : AngledFaces

    private sealed class State {
        object Start : State()
        object DetectSmile : State()
        object DetectAngledFaces : State()
        object CheckAreAllImagesSamePerson : State()
        object Error : State()
        object Finished : State()
    }

    private sealed class Event {
        object Start : Event()
        object DetectSmile : Event()
        object DetectAngledFaces : Event()
        object Error : Event()
        object Finish : Event()
        object Reset : Event()
    }

    private val _faceList = mutableListOf<Bitmap>()
    override val faceList: List<Bitmap> get() = _faceList.toList()

    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationStateFlow: StateFlow<VerificationState> =
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
                if (smileFlow.verificationStateFlow.value == VerificationState.Finished)
                    transitionTo(State.CheckAreAllImagesSamePerson)
                else transitionTo(State.DetectSmile)
            }
            onEnter {
                angledFacesFlow.initialise()
                Log.d("Angled and smile TEST", "Detecting Angled Faces")
            }
        }
        state<State.DetectSmile> {
            on<Event.DetectAngledFaces> {
                if (angledFacesFlow.verificationStateFlow.value == VerificationState.Finished)
                    transitionTo(State.CheckAreAllImagesSamePerson)
                else transitionTo(State.DetectAngledFaces)
            }
            onEnter {
                smileFlow.initialise()
                Log.d("Angled and smile TEST", "Detecting Smiles")

            }
        }
        state<State.CheckAreAllImagesSamePerson> {
            onEnter {
                Log.d("Angled and smile TEST", "Checking face similarity")
                _verificationStateFlow.update {
                    VerificationState.Working("Please wait for additional checks")
                }
            }
            on<Event.Finish> {
                transitionTo(State.Finished)
            }
            on<Event.Error> {
                transitionTo(State.Error)
            }
        }
        state<State.Error> {
            onEnter {
                Log.d("Angled and smile TEST", "Error different persons")
                _verificationStateFlow.update {
                    VerificationState.Error("Different person in at least one of the images")
                }
            }
            on<Event.DetectSmile> {
                transitionTo(State.DetectSmile)
            }
            on<Event.DetectAngledFaces> {
                transitionTo(State.DetectAngledFaces)
            }
        }
        state<State.Finished> {
            onEnter {
                Log.d("Angled and smile TEST", "Finished")
                _verificationStateFlow.update {
                    VerificationState.Finished
                }
            }
            on<Event.DetectSmile> {
                transitionTo(State.DetectSmile)
            }
            on<Event.DetectAngledFaces> {
                transitionTo(State.DetectAngledFaces)
            }
        }
    }

    override fun initialise() {
        _verificationStateFlow.update {
            VerificationState.Start
        }
        smileFlow = Smile()
        angledFacesFlow = AngledFaces(faceRecognition, false)

        _faceList.clear()
        val shouldStartWithSmile = Random.nextBoolean()
        if (shouldStartWithSmile) {
            machine.transition(Event.DetectSmile)
        } else {
            machine.transition(Event.DetectAngledFaces)
        }
        merge(smileFlow.verificationStateFlow, angledFacesFlow.verificationStateFlow).filter {
            it is VerificationState.Working || it is VerificationState.Error
        }.onEach { verificationState ->
            _verificationStateFlow.update {
                verificationState
            }
        }.launchIn(coroutineScope)
    }

    override suspend fun invokeVerificationFlow(face: FaceDetected) {
        when (machine.state) {
            State.DetectAngledFaces -> {
                angledFacesFlow.invokeVerificationFlow(face)
                if (angledFacesFlow.verificationStateFlow.value == VerificationState.Finished)
                    machine.transition(Event.DetectSmile)
            }

            State.DetectSmile -> {
                smileFlow.invokeVerificationFlow(face)
                if (smileFlow.verificationStateFlow.value == VerificationState.Finished)
                    machine.transition(Event.DetectAngledFaces)
            }

            State.CheckAreAllImagesSamePerson -> {
                _faceList.addAll(smileFlow.faceList)
                Log.d("Angled and smile TEST", "smile SIZE ${smileFlow.faceList.size}")
                _faceList.addAll(angledFacesFlow.faceList)
                Log.d("Angled and smile TEST", "angledFaces SIZE ${angledFacesFlow.faceList.size}")
                Log.d("Angled and smile TEST", "LIST SIZE ${faceList.size}")
                if (areAllImagesSamePerson(faceList)) {
                    machine.transition(Event.Finish)
                } else {
                    machine.transition(Event.Error)
                }
            }

            State.Finished -> {
                //TODO
            }

            else -> {
                //TODO
            }
        }
    }

    private fun areAllImagesSamePerson(faceList: List<Bitmap>): Boolean {
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