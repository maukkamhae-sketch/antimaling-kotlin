plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.antimaling.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.antimaling.app"
        minSdk = 24
        targetSdk = 34
        // versionCode/versionName bisa dioverride saat build dari GitHub Actions
        // (lihat release.yml), supaya tiap rilis punya nomor yang naik terus.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0"
    }

    // Kunci penandatanganan release: nilainya datang dari environment variable,
    // yang diisi workflow release.yml dari GitHub Secrets. Kalau variabel itu
    // kosong (misalnya build debug biasa), signingConfig release tidak dipakai.
    val relKeystore = System.getenv("RELEASE_KEYSTORE_PATH")
    val relStorePass = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val relKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val relKeyPass = System.getenv("RELEASE_KEY_PASSWORD")

    signingConfigs {
        if (!relKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(relKeystore)
                storePassword = relStorePass
                keyAlias = relKeyAlias
                keyPassword = relKeyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!relKeystore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Dibutuhkan MainActivity.kt: ActivityMainBinding
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Tidak pakai library HTTP tambahan (Retrofit dkk) supaya skeleton ini ringan;
    // panggilan ke backend pakai HttpURLConnection bawaan Android (lihat net/Api.kt).

    // Dibutuhkan Prefs.kt: MasterKey, EncryptedSharedPreferences (enkripsi api key & PIN)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Dibutuhkan LockScreenActivity.kt: ProcessCameraProvider, ImageCapture (ambil foto pencuri)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
}
