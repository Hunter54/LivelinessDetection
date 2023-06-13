package com.ionutv.livelinesdetection.features.camera

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption

@Composable
public fun Detection(
    detectionOption: LivelinessDetectionOption,
    onSuccessAlertDismissed: () -> Unit,
    onErrorAlertDismissed: () -> Unit = {},
    debugMode: Boolean = false,
) {
    Box(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val viewModel: CameraViewModel =
            viewModel(factory = CameraViewModelFactory(application))
        LaunchedEffect(Unit) { viewModel.updateDetectionOption(detectionOption) }
        DetectionAndCameraPreview(
            cameraViewModel = viewModel,
            onSuccessAlertDismissed = onSuccessAlertDismissed,
            onErrorAlertDismissed = onErrorAlertDismissed
        )
        if (debugMode) {
            val selectedOption by viewModel.detectionOption.collectAsState()
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LivelinessDetectionOptionSelection(
                    list =
                    LivelinessDetectionOption.values().toList(),
                    onSelectedChanged = viewModel::updateDetectionOption,
                    selectedOption = selectedOption
                )
            }
        }
    }
}