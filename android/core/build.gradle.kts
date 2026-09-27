import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Everything that isn't Android UI: talking to the Apps Script, and the rules the web pages
// follow (duplicates, CSV, cell totals, stock maths, the weekly plan). Plain Kotlin, so it is
// unit-tested on any JVM.
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
