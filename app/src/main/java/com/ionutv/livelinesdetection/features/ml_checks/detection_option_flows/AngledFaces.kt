package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import android.util.Log
import com.ionutv.livelinesdetection.features.ml_checks.ImageClassifierService
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition
import com.ionutv.livelinesdetection.features.ml_checks.face_recognition.FaceNetFaceRecognition.Companion.isImageSamePerson
import com.ionutv.livelinesdetection.utils.elementPairs
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.absoluteValue

internal class AngledFaces(private val faceRecognition: FaceNetFaceRecognition) : VerificationFlow {

    private sealed class State() {
        object Start : State()
        object DetectingFaceStraight : State()
        object DetectingLeftSide : State() {
            const val requiredFaceAngle: Int = -25
        }

        object DetectingRightSide : State() {
            const val requiredFaceAngle: Int = 25
        }

        object CheckAreAllImagesSame : State()
        object Error : State()
        object Finished : State()
    }

    private val faceList: MutableList<Bitmap> = mutableListOf()

    private sealed class Event {
        object Start : Event()
        object Detected : Event()
        object Error : Event()
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
            onEnter {
                Log.d("AngledFaces TEST", "DetectingStraight")
                _verificationStateFlow.update {
                    VerificationState.Working("Please look at the camera straight")
                }
            }

            on<Event.Detected> {
                transitionTo(State.DetectingLeftSide)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.DetectingLeftSide> {
            onEnter {
                Log.d("AngledFaces TEST", "DetectingLeftSide")
                _verificationStateFlow.update {
                    VerificationState.Working("Please show left side of face")
                }
            }

            on<Event.Detected> {
                transitionTo(State.DetectingRightSide)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.DetectingRightSide> {
            onEnter {
                Log.d("AngledFaces TEST", "DetectingRightSide")
                _verificationStateFlow.update {
                    VerificationState.Working("Please show right side of face")
                }
            }
            on<Event.Detected> {
                transitionTo(State.CheckAreAllImagesSame)
            }
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.CheckAreAllImagesSame> {
            onEnter {
                Log.d("AngledFaces TEST", "Checking face similarity")
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
            on<Event.Reset> {
                transitionTo(State.Start)
            }
        }
        state<State.Error> {
            onEnter {
                _verificationStateFlow.update {
                    VerificationState.Error("Different person in at least one of the images")
                }
            }
        }
        state<State.Finished> {
            onEnter {
                Log.d("AngledFaces TEST", "Finished detecting")

                _verificationStateFlow.update {
                    VerificationState.Finished
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun invokeVerificationFlow(face: FaceDetected) {
        machine.transition(Event.Start)
        when (val state = machine.state) {
            is State.DetectingFaceStraight -> {
                if (face.headAngle.absoluteValue < 5) {
                    val croppedBitmap =
                        ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
                    faceList.add(croppedBitmap)
                    machine.transition(Event.Detected)
                }
            }

            is State.DetectingLeftSide -> {
                if (face.headAngle in (state.requiredFaceAngle - 5)..state.requiredFaceAngle + 5) {
                    val croppedBitmap =
                        ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
                    faceList.add(croppedBitmap)
                    machine.transition(Event.Detected)
                }
            }

            is State.DetectingRightSide -> {
                if (face.headAngle in (state.requiredFaceAngle - 5)..state.requiredFaceAngle + 5) {
                    val croppedBitmap =
                        ImageClassifierService.cropBitmapExample(face.image, face.boundaries)
                    faceList.add(croppedBitmap)
                    machine.transition(Event.Detected)
                }
            }

            is State.CheckAreAllImagesSame -> {
                val processedImages = faceList.map {
                    faceRecognition.getImageProcessing(it)
                }
                val isDifferentPersonInList = elementPairs(processedImages).map {
                    isImageSamePerson(FaceNetFaceRecognition.MetricUsed.L2, it)
                }.any {
                    !it
                }
                if (isDifferentPersonInList) {
                    machine.transition(Event.Error)
                } else {
                    machine.transition(Event.Detected)
                }
            }

            State.Start -> {
                //TODO
            }

            State.Error -> {
                //TODO
            }

            State.Finished -> {
                //TODO
            }

        }
    }
}

