plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.operatorqrapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.operatorqrapp_v2"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // ZXing QR Scanner (ScanContract exists here)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Required for ScanContract
    implementation("androidx.activity:activity-ktx:1.8.2")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.13.1")

    // Network
    implementation("com.android.volley:volley:1.2.1")

    // UI
    implementation("com.google.android.material:material:1.10.0")
}
