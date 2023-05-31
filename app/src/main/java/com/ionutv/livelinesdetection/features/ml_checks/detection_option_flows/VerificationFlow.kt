package com.ionutv.livelinesdetection.features.ml_checks.detection_option_flows

import com.ionutv.livelinesdetection.features.ml_checks.face_detection.FaceDetected

internal interface VerificationFlow {
    suspend fun startVerificationFlow(face: FaceDetected)
}