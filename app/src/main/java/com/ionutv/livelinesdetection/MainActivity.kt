package com.ionutv.livelinesdetection

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.ionutv.livelinesdetection.features.camera.CameraViewModel
import com.ionutv.livelinesdetection.features.camera.DetectionAndCameraPreview
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption
import com.ionutv.livelinesdetection.permissions.Permission
import com.ionutv.livelinesdetection.ui.theme.LivelinesDetectionTheme
import com.ionutv.livelinesdetection.utils.openApplicationSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LivelinesDetectionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        val context = LocalContext.current

        Surface(modifier = Modifier.padding(paddingValues)) {
            Permission(
                permission = android.Manifest.permission.CAMERA,
                permissionNotAvailableContent = {
                    Greeting(name = "No permission available")
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
                    var selectedOption by remember {
                        mutableStateOf(LivelinessDetectionOption.SMILE)
                    }
                    Box(Modifier.fillMaxSize()) {
                        val context = LocalContext.current
                        val application = context.applicationContext as Application
                        val viewModel =
                            CameraViewModel(application, selectedOption, isDebugMode = false)
                        DetectionAndCameraPreview(
                            livelinessDetectionOption = selectedOption,
                            cameraViewModel = viewModel
                        )
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LivelinessDetectionOptionSelection(
                                list =
                                LivelinessDetectionOption.values().toList(),
                                onSelectedChanged = {
                                    selectedOption = it
                                },
                                selectedOption = selectedOption
                            )
                        }
                    }

                })
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LivelinessDetectionOptionSelection(
    list: List<LivelinessDetectionOption>,
    onSelectedChanged: (LivelinessDetectionOption) -> Unit,
    selectedOption: LivelinessDetectionOption
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        expanded = it
    }) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOption.name,
            onValueChange = { },
            label = { Text("Categories") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            list.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption.name) },
                    onClick = {
                        onSelectedChanged(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LivelinesDetectionTheme {
        Greeting("Android")
    }
}