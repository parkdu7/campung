plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    //firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.shinhan.campung"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.shinhan.campung"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] =
            project.findProperty("NAVER_MAP_CLIENT_ID") ?: ""
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
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    hilt {
        enableAggregatingTask = false
    }
    
}


dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.play.services.maps)
    debugImplementation(libs.compose.tooling)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.material)

    // Hilt + KSP
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room + KSP
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //firebase
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    // Navigation-Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // DataStore (토큰 저장)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 코루틴에서 Task.await() 쓰려면 (선택)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    //화면이동
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    //네이버지도
    implementation("com.naver.maps:map-sdk:3.22.1") // 네이버 지도 SDK
    implementation("com.google.android.gms:play-services-location:21.3.0") // 위치
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4") // Compose-Lifecycle
}
