package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.flow.flowOf


internal class Smile() : VerificationFlow {

    internal sealed interface State{
        object Detecting : State
        object SmileDetected : State

    }

    private val verificationFlow = flowOf(State.Detecting,State.SmileDetected)
    override suspend fun startVerificationFlow(faceClassified: FaceDetected) {
//        verificationFlow.collect{
//            when(it){
//                State.Detecting -> {
//                    if(faceClassified.smiling)
//                }
//                State.SmileDetected -> TODO()
//            }
//        }
//          if(faceClassified.smiling)
    }

}