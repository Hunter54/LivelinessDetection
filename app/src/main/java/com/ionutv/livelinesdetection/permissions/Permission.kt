package com.ionutv.livelinesdetection.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
public fun Permission(
    permission: String,
    content: @Composable () -> Unit,
    rationale: String = "This permission is important for this app. Please grant the permission.",
    permissionNotAvailableContent: @Composable () -> Unit = { },
    ) {
    val permissionState = rememberPermissionState(permission)

    when {
        permissionState.status.isGranted -> {
            content()
        }

        permissionState.status.shouldShowRationale -> {
            Rationale(text = rationale) {
                permissionState.launchPermissionRequest()
            }
        }

        else -> {
            SideEffect {
                permissionState.launchPermissionRequest()
            }
            permissionNotAvailableContent()
        }
    }
}

@Composable
private fun Rationale(
    text: String,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Don't */ },
        title = {
            Text(text = "Permission request")
        },
        text = {
            Text(text)
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Ok")
            }
        }
    )
}