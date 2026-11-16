plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val dataRepo = (project.findProperty("dataRepo") as String?)?.ifBlank { null } ?: "qmeow-studio/hk-report-covers"
val dataToken = (project.findProperty("dataToken") as String?) ?: ""
val verCode = ((project.findProperty("verCode") as String?)?.ifBlank { null } ?: "1").toInt()

android {
    namespace = "com.tx.reportcovers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tx.reportcovers"
        minSdk = 29
        targetSdk = 35
        versionCode = verCode
        versionName = "1.0.$verCode"
        buildConfigField("String", "DATA_REPO", "\"$dataRepo\"")
        buildConfigField("String", "DATA_TOKEN", "\"$dataToken\"")
    }
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "b1a582da67ce9f69"
            keyAlias = "key"
            keyPassword = "b1a582da67ce9f69"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
