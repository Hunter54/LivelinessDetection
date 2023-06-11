package com.ionutv.livelinesdetection.features.ml_checks

public enum class LivelinessDetectionOption() {
    SMILE(),
    //    BLINK(::Smile),
//    SMILE_BLINK(::Smile),
    RANDOM_EMOTION(),

    //    RANDOM_EMOTION_BLINK(::Smile),
    ANGLED_FACES(),
    ANGLED_FACES_WITH_SMILE(),
    ANGLED_FACES_WITH_EMOTION(),
//    ANGLED_FACES_WITH_EMOTION_BLINK(::Smile),
}