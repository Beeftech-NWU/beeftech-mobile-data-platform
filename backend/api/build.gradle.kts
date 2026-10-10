plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("application")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.beeftech.backend.api.ApplicationKt")
}

// `-D` flags on the Gradle command line only reach the Gradle JVM, not the forked app JVM.
// Forward the beeftech.* properties (e.g. beeftech.seed.dev, beeftech.db.url) to `run`.
tasks.named<JavaExec>("run") {
    System.getProperties()
        .filterKeys { (it as String).startsWith("beeftech.") }
        .forEach { (key, value) -> systemProperty(key as String, value) }
}

dependencies {
    implementation(libs.jakarta.mail)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)

    implementation(libs.logback.classic)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.java.jwt)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)

    implementation(libs.sqlite.jdbc)
    implementation(libs.pdfbox)
    implementation(libs.jbcrypt)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}
