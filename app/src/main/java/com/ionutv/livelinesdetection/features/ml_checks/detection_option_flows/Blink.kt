package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import com.tinder.StateMachine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

internal class Blink : VerificationFlow {

    sealed class BlinkVerificationEvent {
        object EyesOpen : BlinkVerificationEvent()
        object EyesClodes : BlinkVerificationEvent()
        object Reset : BlinkVerificationEvent()
    }

    sealed class DetectStates {
        object WaitingInitialEyesOpen : DetectStates()
        object WaitingEyesClosed : DetectStates()
        object WaitingEyesOpen : DetectStates()
        object BlinkDetected : DetectStates()
    }

    private var lastFaceTimeStamp: Long? = null


    private val detectFlow = flowOf(
        DetectStates.WaitingInitialEyesOpen,
        DetectStates.WaitingEyesClosed,
        DetectStates.WaitingEyesOpen
    )
    private var currentDetectFlow = detectFlow
    private val currentDetectState =
        MutableStateFlow<DetectStates>(DetectStates.WaitingInitialEyesOpen)

    private val _faceList = mutableListOf<Bitmap>()
    override val faceList: List<Bitmap> get() = _faceList.toList()
    private val _verificationStateFlow =
        MutableStateFlow<VerificationState>(VerificationState.Start)
    override val verificationFlowState: StateFlow<VerificationState> =
        _verificationStateFlow.asStateFlow()

    override suspend fun invokeVerificationFlow(faceClassified: FaceDetected) {
        val machine = StateMachine.create<DetectStates, BlinkVerificationEvent, Nothing> {
            initialState(DetectStates.WaitingInitialEyesOpen)
            state<DetectStates.WaitingInitialEyesOpen> {
                on<BlinkVerificationEvent.EyesOpen> {
                    transitionTo(DetectStates.WaitingEyesClosed)
                }
            }
            state<DetectStates.WaitingEyesClosed> {
                on<BlinkVerificationEvent.EyesClodes> {
                    transitionTo(DetectStates.WaitingEyesOpen)
                }
                on<BlinkVerificationEvent.Reset> {
                    transitionTo(DetectStates.WaitingInitialEyesOpen)
                }
            }
            state<DetectStates.WaitingEyesOpen> {
                on<BlinkVerificationEvent.Reset> {
                    transitionTo(DetectStates.WaitingInitialEyesOpen)
                }
            }
        }
    }

}