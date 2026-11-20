plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ferrovelho.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ferrovelho.game"
        minSdk = 24
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.2"
    }

    // Chave fixa: todo APK novo usa a mesma assinatura e atualiza por cima do instalado.
    signingConfigs {
        create("fixa") {
            storeFile = file("../ferrovelho.jks")
            storePassword = "ferrovelho123"
            keyAlias = "ferrovelho"
            keyPassword = "ferrovelho123"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("fixa") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixa")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
}
