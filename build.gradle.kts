// Project level build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Hilt
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // KSP (for Room + Hilt annotation processing)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}