package com.ionutv.livelinesdetection.utils

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun DisplayAlert(
    title: String,
    message: String,
    onAlertDismissed: () -> Unit
) {
    var shouldShowDialog by remember {
        mutableStateOf(true)
    }
    if (shouldShowDialog) {
        AlertDialog(
            onDismissRequest = { shouldShowDialog = false },
            confirmButton = {
                Button(onClick = {
                    onAlertDismissed()
                    shouldShowDialog = false
                }) {
                    Text(text = "OK")
                }
            },
            title = { Text(title) },
            text = { Text(message) })
    }
}