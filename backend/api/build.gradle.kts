plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("application")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.beeftech.backend.api.ApplicationKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.auth0:java-jwt:4.4.0")
}

tasks.test {
    useJUnitPlatform()
}