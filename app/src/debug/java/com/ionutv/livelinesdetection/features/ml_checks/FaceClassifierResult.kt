package com.ionutv.livelinesdetection.features.ml_checks

import android.graphics.Bitmap
import android.graphics.Rect

internal sealed interface FaceClassifierResult {
    object NoFaceDetected : FaceClassifierResult
    data class Error(val message: String) : FaceClassifierResult
    data class FaceClassified(
        val boundaries: Rect,
        val image: Bitmap,
        val croppedImage: Bitmap,
        val faceAngle: Int,
        val emotions: List<ClassifierResult>,
        val name: String = ""
    ) : FaceClassifierResult
}