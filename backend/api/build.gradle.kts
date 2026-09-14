plugins {
    id("org.jetbrains.kotlin.jvm")
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
}

tasks.test {
    useJUnitPlatform()
}