import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.beeftech.database"

    compileSdk = 35

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

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

    // Room
    // 'api' is required because BeefTechDatabase publicly
    // extends androidx.room.RoomDatabase.
    api("androidx.room:room-runtime:2.8.4")

    implementation("androidx.room:room-ktx:2.8.4")

    ksp("androidx.room:room-compiler:2.8.4")

    // SQLCipher
    implementation("net.zetetic:sqlcipher-android:4.17.0@aar")

    // SQLite
    implementation("androidx.sqlite:sqlite:2.6.2")

    // Security
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("androidx.security:security-crypto:1.1.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")


    // Android tests
    androidTestImplementation(
        "androidx.test.ext:junit:1.3.0"
    )

    androidTestImplementation(
        "androidx.test:core:1.7.0"
    )

    androidTestImplementation(
        "androidx.test:runner:1.7.0"
    )
}