plugins {
    alias(libs.plugins.kotlin).version(libs.versions.ksp).apply(false)
}

buildscript {
    dependencies {
        classpath(libs.plugins.kotlin.gradle.plugin)
    }
}
