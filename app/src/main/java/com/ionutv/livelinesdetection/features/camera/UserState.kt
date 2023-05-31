package com.ionutv.livelinesdetection.features.camera

internal sealed class UserState {
    object START : UserState()
    data class Guiding(val message: String) : UserState()
    object END : UserState()
}