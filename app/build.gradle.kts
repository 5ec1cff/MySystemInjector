plugins {
    id("com.android.application")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "five.ec1cff.mysysteminjector"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    namespace = "five.ec1cff.mysysteminjector"
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}