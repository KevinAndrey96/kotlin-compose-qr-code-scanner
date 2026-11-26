import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

kover {
    currentProject {
        createVariant("coverage") {
            add("jvm")
        }
    }
}

dependencies {
    implementation(libs.zxing.core)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit)
}
