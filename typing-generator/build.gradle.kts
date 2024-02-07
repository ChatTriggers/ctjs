buildscript {
    dependencies {
        classpath(libs.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ksp)
}
