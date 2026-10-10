import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.beeftech.database"

    compileSdk = 35

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
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
    api(libs.androidx.room.runtime)

    implementation(libs.androidx.room.ktx)

    // Shared process-wide runtime UI state
    api(libs.kotlinx.coroutines.core)

    ksp(libs.androidx.room.compiler)

    // SQLCipher (the @aar suffix cannot be expressed in a catalog entry)
    implementation("net.zetetic:sqlcipher-android:${libs.versions.sqlcipher.get()}@aar")

    // SQLite
    implementation(libs.androidx.sqlite)

    // Security
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.androidx.security.crypto)

    // Unit tests
    testImplementation(libs.junit)

    // Android tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
}
