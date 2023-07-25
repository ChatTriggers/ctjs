plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.jetbrains.dokka:dokka-core:1.8.20")
    compileOnly("org.jetbrains.dokka:dokka-base:1.8.20")
}
