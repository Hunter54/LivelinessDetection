package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.flow.StateFlow

internal sealed interface VerificationState
internal interface VerificationFlow {
    val verificationFlowState : StateFlow<VerificationState>
    suspend fun startVerificationFlow(face: FaceDetected)
}