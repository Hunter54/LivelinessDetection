package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import android.graphics.Bitmap
import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected
import kotlinx.coroutines.flow.StateFlow
internal sealed class VerificationState{
    object Start : VerificationState()
    data class Working(val message : String) : VerificationState()
    object Finished : VerificationState()
    data class Error(val message: String): VerificationState()
}
internal interface VerificationFlow {
    val faceList : List<Bitmap>
    val verificationStateFlow : StateFlow<VerificationState>
    fun initialise()
    suspend fun invokeVerificationFlow(face: FaceDetected)
}