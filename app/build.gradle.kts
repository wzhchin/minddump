plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.room3)
}

android {
    namespace = "com.chin.minddump"
    val sdkVersion = libs.versions.compileSdk
        .get()
        .toInt()
    compileSdk = sdkVersion

    defaultConfig {
        applicationId = "com.chin.minddump"
        minSdk = libs.versions.minSdk
            .get()
            .toInt()
        val targetSdkVersion = libs.versions.targetSdk
            .get()
            .toInt()
        targetSdk = targetSdkVersion
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("MINDUMP_KEYSTORE_PASSWORD") ?: ""
            keyAlias = "minddump"
            keyPassword = System.getenv("MINDUMP_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt-config.yml"))
}

ktlint {
    android.set(true)
    filter {
        exclude { element -> element.file.path.contains("generated/") }
    }
}

dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Compose (pinned directly, no BOM — material3 1.5.0-alpha21 needs Compose 1.11.x)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)

    // Image loading (Coil 3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Zoomable image preview
    implementation(libs.image.viewer)

    // Biometric
    implementation(libs.biometric)

    // Security (EncryptedSharedPreferences)
    implementation(libs.security.crypto)

    // Logging
    implementation(libs.timber)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room3
    implementation(libs.room3.runtime)
    ksp(libs.room3.compiler)

    // materialKolor — seed-color + palette-style dynamic theming
    implementation(libs.materialkolor)

    // DataStore — theme preferences persistence
    implementation(libs.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room3.testing)
    kspTest(libs.hilt.compiler)
}
