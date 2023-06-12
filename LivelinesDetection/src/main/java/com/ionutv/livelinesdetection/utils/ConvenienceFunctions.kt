package com.ionutv.livelinesdetection.utils

internal fun emptyString(): String = ""

@OptIn(ExperimentalStdlibApi::class)
internal fun <T> elementPairs(arr: List<T>): Sequence<Pair<T, T>> = sequence {
    for (i in 0..<arr.size - 1)
        for (j in i + 1..<arr.size)
            yield(arr[i] to arr[j])
}

internal fun <T> MutableCollection<T>.popOrNull() = with(iterator()) {
    if (hasNext()) {
        next().also { remove() }
    } else null
}