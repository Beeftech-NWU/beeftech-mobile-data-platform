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

    kotlinOptions {
        jvmTarget = "17"
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

dependencies {

    // BeefTech modules
    implementation(project(":android:farm-traceability"))
    implementation(project(":android:calf-registration"))
    implementation(project(":android:farmer-registration"))
    implementation(project(":android:database"))
    implementation(project(":android:feed-crib"))
    implementation(project(":android:authentication"))
    implementation(project(":android:management"))

    // Ktor client
    implementation("io.ktor:ktor-client-core:3.0.3")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Android / Activity
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")

    // WorkManager - centralized scheduled synchronization
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Unit tests
    testImplementation("junit:junit:4.13.2")

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}