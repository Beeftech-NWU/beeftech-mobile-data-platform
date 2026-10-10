import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.beeftech.demoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beeftech.demoapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        /*
         * Debug builds talk to a local backend (10.0.2.2 is the host machine from the emulator).
         * Override with -Pbeeftech.baseUrl=http://<lan-ip>:8081/ for a physical device, or point
         * it at the hosted server to test against that. Release always uses the hosted backend.
         */
        debug {
            buildConfigField(
                "String",
                "BACKEND_BASE_URL",
                "\"${providers.gradleProperty("beeftech.baseUrl").getOrElse("http://10.0.2.2:8081/")}\""
            )
        }
        release {
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://beeftech-backend.onrender.com/\"")
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


    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Fix duplicate META-INF resource error
    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {

    // BeefTech modules
    implementation(project(":android:farm-traceability"))
    implementation(project(":android:calf-registration"))
    implementation(project(":android:farmer-registration"))
    implementation(project(":android:database"))
    implementation(project(":android:feed-crib"))
    implementation(project(":android:authentication"))
    implementation(project(":android:management"))

    // Ktor client (HttpClient is passed to the feature modules' sync clients)
    implementation(libs.ktor.client.core)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Android / Activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    // WorkManager - centralized scheduled synchronization
    implementation(libs.androidx.work.runtime.ktx)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
