import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.counhopig.ccalendar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.counhopig.ccalendar"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val signingPropsFile = rootProject.file("signing.properties")
    val signingProps = mutableMapOf<String, String>()
    if (signingPropsFile.exists()) {
        signingPropsFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val idx = trimmed.indexOf('=')
                if (idx > 0) {
                    val key = trimmed.substring(0, idx).trim()
                    val value = trimmed.substring(idx + 1).trim()
                    signingProps[key] = value
                }
            }
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProps["storeFile"] ?: "my-release-key.jks"
            val storePassword = signingProps["storePassword"] ?: System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            val keyAlias = signingProps["keyAlias"] ?: System.getenv("SIGNING_KEY_ALIAS") ?: ""
            val keyPassword = signingProps["keyPassword"] ?: System.getenv("SIGNING_KEY_PASSWORD") ?: ""

            storeFile = if (file(storeFilePath).exists()) file(storeFilePath) else file("my-release-key.jks")
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        release {
            val releaseConfig = signingConfigs.getByName("release")
            if (!releaseConfig.storePassword.isNullOrEmpty()) {
                signingConfig = releaseConfig
            }
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // implementation(libs.androidx.glance)
    // implementation(libs.androidx.glance.appwidget)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.mnode.ical4j:ical4j:4.0.0-beta1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}