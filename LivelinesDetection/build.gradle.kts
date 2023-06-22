@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    `maven-publish`
}
//val githubPropertiesFile = rootProject.file("github.properties");
//val githubProperties = Properties()
//githubProperties.load(FileInputStream(githubPropertiesFile))
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
    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    run {
                        groupId = "com.ionutv"
                        artifactId = "livelinessdetection"
                        version = "0.2"
                        from(components["release"])
                    }
//                    pom {
//                        withXml {
//                            // add dependencies to pom
//                            val dependencies = asNode().appendNode("dependencies")
//                            configurations.implementation.get().allDependencies.forEach {
//                                if (it.group != null &&
//                                    "unspecified" != it.name &&
//                                    it.version != null
//                                ) {
//
//                                    val dependencyNode = dependencies.appendNode("dependency")
//                                    dependencyNode.appendNode("groupId", it.group)
//                                    dependencyNode.appendNode("artifactId", it.name)
//                                    dependencyNode.appendNode("version", it.version)
//                                }
//                            }
////                            configurations.api.get().allDependencies.forEach {
////                                if (it.group != null &&
////                                    "unspecified" != it.name &&
////                                    it.version != null
////                                ) {
////
////                                    val dependencyNode = dependencies.appendNode("dependency")
////                                    dependencyNode.appendNode("groupId", it.group)
////                                    dependencyNode.appendNode("artifactId", it.name)
////                                    dependencyNode.appendNode("version", it.version)
////                                }
////                            }
//                        }
//                    }
                }
            }
//            repositories {
//                maven {
//                    name = "GitHubPackages"
//                    /** Configure path of your package repository on Github
//                     *  Replace GITHUB_USERID with your/organisation Github userID and REPOSITORY with the repository name on GitHub
//                     */
//                    url =
//                        uri("https://maven.pkg.github.com/Hunter54/LivelinessDetection/") // Github Package
//                    credentials {
//                        //Fetch these details from the properties file or from Environment variables
//                        username =
//                            githubProperties["gpr.user"] as String? ?: System.getenv("GPR_USER")
//                        password = githubProperties["gpr.key"] as String?
//                            ?: System.getenv("GPR_API_KEY")
//                    }
//                }
//            }
        }
    }
    aaptOptions {
        noCompress += "tflite"
    }
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