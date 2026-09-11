plugins {
    alias(libs.plugins.android.application)
}

// Google sample IDs: only serve test ads. Real IDs come from gradle
// properties (like the signing config) once the AdMob account exists.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"

android {
    namespace = "com.github.keiki.displaytorch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.keiki.displaytorch"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        // Erzeugt locales_config.xml aus den vorhandenen values-*-Ordnern, damit
        // Android 13+ die App in der Sprachauswahl der Systemeinstellungen fuehrt.
        generateLocaleConfig = true
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
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$testBannerAdUnitId\"")
        }
        release {
            val admobAppId = findProperty("DISPLAYTORCH_ADMOB_APP_ID") as String? ?: testAdmobAppId
            val bannerAdUnitId = findProperty("DISPLAYTORCH_BANNER_AD_UNIT_ID") as String? ?: testBannerAdUnitId
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$bannerAdUnitId\"")
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
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
