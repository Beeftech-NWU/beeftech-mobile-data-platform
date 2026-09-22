plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.beeftech.farmerregistration"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles(
            "consumer-rules.pro"
        )
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // BeefTech database
    implementation(
        project(":android:database")
    )

    // Compose
    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.activity.ktx
    )

    implementation(
        libs.androidx.annotation
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    implementation(
        libs.androidx.compose.material3.adaptive.navigation.suite
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.constraintlayout
    )

    implementation(
        libs.androidx.core.ktx
    )

    // Lifecycle / ViewModel
    implementation(
        libs.androidx.lifecycle.livedata.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.ktx
    )

    // Material
    implementation(
        libs.material
    )

    // WorkManager - automatic Farmer synchronization
    implementation(
        "androidx.work:work-runtime-ktx:2.9.1"
    )

    // Ktor client - Farmer backend synchronization
    implementation(
        "io.ktor:ktor-client-core:3.0.3"
    )

    implementation(
        "io.ktor:ktor-client-okhttp:3.0.3"
    )

    implementation(
        "io.ktor:ktor-client-content-negotiation:3.0.3"
    )

    implementation(
        "io.ktor:ktor-serialization-kotlinx-json:3.0.3"
    )

    // Kotlin serialization
    implementation(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"
    )

    // Unit tests
    testImplementation(
        libs.junit
    )

    testImplementation(
        "io.ktor:ktor-client-mock:3.0.3"
    )

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
    )

    // Android tests
    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    // Debug
    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}