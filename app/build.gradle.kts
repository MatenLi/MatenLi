import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.rffrr.mateli"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.rffrr.mateli"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

val libboxAar = file("libs/libbox.aar")
val libboxUrl = "https://github.com/singbox-android/libbox/releases/download/1.14.4/libbox.aar"

/** Downloads the sing-box core (libbox 1.14.4) once, before the first build. */
val fetchLibbox by tasks.registering {
    outputs.file(libboxAar)
    doLast {
        if (!libboxAar.exists()) {
            libboxAar.parentFile.mkdirs()
            URI(libboxUrl).toURL().openStream().use { input ->
                libboxAar.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(fetchLibbox)
}

dependencies {
    implementation(files(libboxAar))
    implementation(platform("androidx.compose:compose-bom:2024.11.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
