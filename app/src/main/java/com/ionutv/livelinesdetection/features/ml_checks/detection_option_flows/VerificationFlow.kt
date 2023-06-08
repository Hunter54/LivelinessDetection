package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.flow.StateFlow
internal sealed class VerificationState{
    object Start : VerificationState()
    object Working : VerificationState()
    object Finished : VerificationState()
    data class Error(val message: String): VerificationState()
}
internal interface VerificationFlow {

    val verificationFlowState : StateFlow<VerificationState>
    suspend fun invokeVerificationFlow(face: FaceDetected)
}