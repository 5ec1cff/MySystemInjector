plugins {
    id("com.android.application")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "five.ec1cff.mysysteminjector"
        minSdk = 30
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "five.ec1cff.mysysteminjector"
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly(project(":stub"))
}
