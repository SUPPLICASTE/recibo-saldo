plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val storePass = providers.environmentVariable("RECIBO_STORE_PASSWORD")
    .orElse(providers.gradleProperty("recibo.storePassword")).orNull
    ?: error("Falta RECIBO_STORE_PASSWORD o recibo.storePassword en local.properties")
val keyPass = providers.environmentVariable("RECIBO_KEY_PASSWORD")
    .orElse(providers.gradleProperty("recibo.keyPassword")).orNull
    ?: error("Falta RECIBO_KEY_PASSWORD o recibo.keyPassword en local.properties")
val aliasName = providers.environmentVariable("RECIBO_KEY_ALIAS")
    .orElse(providers.gradleProperty("recibo.keyAlias")).orNull
    ?: "recibosaldo"

android {
    namespace = "com.recibosaldo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.recibosaldo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.3.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("stable") {
            storeFile = file("recibo-saldo.keystore")
            storePassword = storePass
            keyAlias = aliasName
            keyPassword = keyPass
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stable")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("stable")
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
