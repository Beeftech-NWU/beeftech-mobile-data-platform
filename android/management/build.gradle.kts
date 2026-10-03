plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.beeftech.management"
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

    // TokenProvider
    implementation(
        project(":android:database")
    )

    // Compose
    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.core.ktx
    )

    // Lifecycle / ViewModel
    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    // Ktor client - management API
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

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
