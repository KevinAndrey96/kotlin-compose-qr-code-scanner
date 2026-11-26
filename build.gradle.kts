plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        ktlint(ktlintVersion)
    }
}

dependencies {
    kover(project(":core"))
    kover(project(":app"))
}

kover {
    currentProject {
        createVariant("coverage") {}
    }
    reports {
        filters {
            excludes {
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
                classes(
                    "*.BuildConfig",
                    "*ComposableSingletons*",
                    "*_Impl*",
                    "com.kevinandrey.qrscanner.MainActivity*",
                    "com.kevinandrey.qrscanner.ui.scanner.CameraPreviewKt*",
                )
            }
        }
        variant("coverage") {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}
