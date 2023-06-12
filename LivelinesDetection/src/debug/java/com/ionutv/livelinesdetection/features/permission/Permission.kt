package com.ionutv.livelinesdetection.features.permission

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ionutv.livelinesdetection.features.camera.Detection
import com.ionutv.livelinesdetection.permissions.Permission
import com.ionutv.livelinesdetection.utils.openApplicationSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionWithPermission() {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        val context = LocalContext.current

        Surface(modifier = Modifier.padding(paddingValues)) {
            Permission(
                permission = Manifest.permission.CAMERA,
                permissionNotAvailableContent = {
                    Text(text = "No permission available")
                    LaunchedEffect(key1 = null) {
                        val result = snackbarHostState.showSnackbar(
                            message = "The permission is needed for the app to work",
                            duration = SnackbarDuration.Indefinite,
                            actionLabel = "Open Settings"
                        )
                        when (result) {
                            SnackbarResult.Dismissed -> {}
                            SnackbarResult.ActionPerformed -> {
                                context.openApplicationSettings()
                            }
                        }
                    }
                },
                content = {
                    Log.d("COMPOSE TEST", "calling detection and preview function")
                    Detection()
                })
        }
    }
}