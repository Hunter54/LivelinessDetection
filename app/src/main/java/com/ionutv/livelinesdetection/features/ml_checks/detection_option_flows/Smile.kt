package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.update


internal class Smile : VerificationFlow {
    internal sealed class SmileVerificationState: VerificationState{
        object Start : SmileVerificationState()
        object Detecting : SmileVerificationState()
        object SmileDetected : SmileVerificationState()
    }



    private val verificationFlow = flowOf(SmileVerificationState.Start, SmileVerificationState.Detecting,SmileVerificationState.SmileDetected)
    private val _verificationStateFlow = MutableStateFlow<SmileVerificationState>(SmileVerificationState.Start)
    override val verificationFlowState: StateFlow<SmileVerificationState> = _verificationStateFlow.asStateFlow()

    override suspend fun startVerificationFlow(faceClassified: FaceDetected) {
        when(verificationFlowState.value){
            SmileVerificationState.Start ->{
                nextState()
            }
            SmileVerificationState.Detecting -> {
                if(faceClassified.smiling == true){
                    nextState()
                }
            }
            SmileVerificationState.SmileDetected -> {

            }
        }
    }

    private suspend fun nextState(){
        _verificationStateFlow.update {
            verificationFlow.onEmpty {
                SmileVerificationState.SmileDetected
            }.first()
        }
    }

}