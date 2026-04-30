plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)

    // REQUIRED (you were missing these)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.safistep"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.safistep"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "BASE_URL", "\"https://safistep.codejar.co.ke/\"")
        buildConfigField("String", "APP_VERSION", "\"1.0\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // ---------------- CORE ----------------
    implementation(libs.androidxCoreKtx)
    implementation(libs.lifecycleRuntime)
    implementation(libs.activityCompose)

    // ---------------- SPLASH / BIOMETRIC ----------------
    implementation(libs.androidxSplash)
    implementation(libs.androidxBiometric)

    // ---------------- COMPOSE ----------------
    implementation(platform(libs.composeBomLib))

    implementation(libs.composeUi)
    implementation(libs.composeUiGraphics)
    implementation(libs.composeUiToolingPreview)

    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIcons)
    implementation(libs.composeAnimation)
    implementation(libs.composeFoundation)

    debugImplementation(libs.composeUiTooling)

    // ---------------- NAVIGATION ----------------
    implementation(libs.navigationCompose)
    implementation(libs.hiltNavigationCompose)

    // ---------------- HILT ----------------
    implementation(libs.hiltAndroid)
    ksp(libs.hiltCompiler)

    // ---------------- ROOM ----------------
    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    ksp(libs.roomCompiler)

    // ---------------- NETWORK ----------------
    implementation(libs.retrofit)
    implementation(libs.retrofitMoshi)

    implementation(platform(libs.okhttpBom))
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)

    implementation(libs.moshi)
    ksp(libs.moshiCompiler)

    // ---------------- DATASTORE ----------------
    implementation(libs.datastore)

    // ---------------- COROUTINES ----------------
    implementation(libs.coroutinesAndroid)

    // ---------------- UI ----------------
    implementation(libs.lottie)
    implementation(libs.coil)
    implementation(libs.accompanistPermissions)
    implementation(libs.accompanistSystemUi)

    // ---------------- WORK MANAGER ----------------
    implementation(libs.workRuntime)
    implementation(libs.hiltWork)
    ksp(libs.hiltWorkCompiler)

    // ---------------- TEST ----------------
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspresso)

    androidTestImplementation(platform(libs.composeBomLib))
//  androidTestImplementation(libs.composeUiTestJunit4)
//
//  debugImplementation(libs.composeUiTestManifest)
}