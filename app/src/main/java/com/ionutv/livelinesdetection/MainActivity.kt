package com.ionutv.livelinesdetection

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import com.ionutv.livelinesdetection.features.camera.DetectionAndCameraPreview
import com.ionutv.livelinesdetection.permissions.Permission
import com.ionutv.livelinesdetection.ui.theme.LivelinesDetectionTheme
import com.ionutv.livelinesdetection.utils.openApplicationSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LivelinesDetectionTheme {
                // A surface container using the 'background' color from the theme
                MainScreen()
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
                    Box(Modifier.fillMaxSize()){ DetectionAndCameraPreview() }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LivelinesDetectionTheme {
        Greeting("Android")
    }
}