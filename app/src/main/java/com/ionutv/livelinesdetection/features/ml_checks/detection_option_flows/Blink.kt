package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.update
import ru.nsk.kstatemachine.DefaultState
import ru.nsk.kstatemachine.Event
import ru.nsk.kstatemachine.FinalState
import ru.nsk.kstatemachine.addFinalState
import ru.nsk.kstatemachine.addInitialState
import ru.nsk.kstatemachine.createStateMachine
import ru.nsk.kstatemachine.initialState
import ru.nsk.kstatemachine.transition
import ru.nsk.kstatemachine.transitionOn

internal class Blink : VerificationFlow{

    sealed class BlinkVerificationState : VerificationState{
        object Start: BlinkVerificationState()
        object Detecting : BlinkVerificationState()
        object BlinkDetected : BlinkVerificationState()
    }
    object SwitchEvent : Event
    sealed class DetectStates : DefaultState(){
        object WaitingInitialEyesOpen : DetectStates()
        object WaitingEyesClosed: DetectStates()
        object WaitingEyesOpen: DetectStates()
        object BlinkDetected: DetectStates(), FinalState
    }

    private var lastFaceTimeStamp : Long? = null
    private val verificationFlow = flowOf(BlinkVerificationState.Start,BlinkVerificationState.Detecting,BlinkVerificationState.BlinkDetected)


    private val detectFlow = flowOf(DetectStates.WaitingInitialEyesOpen, DetectStates.WaitingEyesClosed, DetectStates.WaitingEyesOpen)
    private var currentDetectFlow = detectFlow
    private val currentDetectState = MutableStateFlow<DetectStates>(DetectStates.WaitingInitialEyesOpen)


    private val _verificationStateFlow = MutableStateFlow<BlinkVerificationState>(BlinkVerificationState.Start)
    override val verificationFlowState: StateFlow<BlinkVerificationState> = _verificationStateFlow.asStateFlow()

    override suspend fun startVerificationFlow(faceClassified: FaceDetected) {
        val machine = createStateMachine(GlobalScope){
            addInitialState(DetectStates.WaitingInitialEyesOpen){
                transition<SwitchEvent> {
                    lastFaceTimeStamp = faceClassified.processedTimeStamp
                    targetState = DetectStates.WaitingEyesClosed
                }
            }

            addState(DetectStates.WaitingEyesClosed){
                transition<SwitchEvent> {
                    if(faceClassified.eyesOpen == false){
                        targetState = DetectStates.WaitingEyesOpen
                    }
                }
            }
            addState(DetectStates.WaitingEyesOpen){
                transition<SwitchEvent>{
                    if(faceClassified.eyesOpen == true){
                        targetState = DetectStates.BlinkDetected
                    }
                }
            }

            addFinalState(DetectStates.BlinkDetected)
        }
        machine.start()

//        when(verificationFlowState.value){
//            BlinkVerificationState.Start ->{
//                nextVerificationState()
//            }
//            BlinkVerificationState.Detecting -> {
//                when(currentDetectState.value){
//                    DetectStates.WaitingEyesClosed -> {
//                        if(faceClassified.eyesOpen == true){
//                            currentDetectFlow = detectFlow
//                        }
//                    }
//                    DetectStates.WaitingEyesOpen -> {
//
//                    }
//                    DetectStates.WaitingInitialEyesOpen -> {
//                        if(faceClassified.eyesOpen == true){
//                            nextDetectState()
//                            lastFaceTimeStamp = faceClassified.processedTimeStamp
//                        }
//                    }
//                }
//            }
//            BlinkVerificationState.BlinkDetected -> {
//
//            }
//        }
    }
    private suspend fun nextDetectState(){
        currentDetectState.update {
            currentDetectFlow.onEmpty {
                BlinkVerificationState.BlinkDetected
            }.first()
        }
    }
    private suspend fun nextVerificationState(){
        _verificationStateFlow.update {
            verificationFlow.onEmpty {
                BlinkVerificationState.BlinkDetected
            }.first()
        }
    }

}