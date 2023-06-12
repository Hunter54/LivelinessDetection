@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
}

android {
    namespace = "com.ionutv.livelinessdetection"
    compileSdk = 33

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xcontext-receivers", "-Xexplicit-api=warning")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources.excludes.add("META-INF/notice.txt")
    }
//    androidResources {
//        noCompress += "tflite"
//    }
}

dependencies {

    implementation(libs.coil)
    implementation(libs.core.ktx)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.camerax)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    api(libs.google.mlkit.face) {
        isTransitive = true
    }
    api("com.tinder.statemachine:statemachine:0.2.0") {
        isTransitive = true
    }
    api("org.tensorflow:tensorflow-lite:0.0.0-nightly-SNAPSHOT") {
        isTransitive = true
    }
    // The GPU delegate library is optional. Depend on it as needed.
    api("org.tensorflow:tensorflow-lite-gpu:0.0.0-nightly-SNAPSHOT") {
        isTransitive = true
    }
    api("org.tensorflow:tensorflow-lite-support:0.0.0-nightly-SNAPSHOT") {
        isTransitive = true
    }
    api("org.tensorflow:tensorflow-lite-gpu-api:0.0.0-nightly-SNAPSHOT") {
        isTransitive = true
    }
}