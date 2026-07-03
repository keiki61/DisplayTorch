plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.keiki.displaytorch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.keiki.displaytorch"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    val releaseStoreFile = (findProperty("DISPLAYTORCH_STORE_FILE") as String?)?.let(::file)
    val releaseStorePassword = findProperty("DISPLAYTORCH_STORE_PASSWORD") as String?
    val releaseKeyAlias = findProperty("DISPLAYTORCH_KEY_ALIAS") as String?
    val releaseKeyPassword = findProperty("DISPLAYTORCH_KEY_PASSWORD") as String?
    val releaseSigningConfigured = releaseStoreFile?.exists() == true &&
        releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
}
