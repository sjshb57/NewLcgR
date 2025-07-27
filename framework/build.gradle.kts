@file:Suppress("DEPRECATION")

plugins {
    id("com.android.library")
    alias(libs.plugins.parcelize)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "top.easelink.framework"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // android support libraries
    api(libs.androidx.appcompat)
    api(libs.androidx.recyclerview)
    api(libs.androidx.cardview)
    api(libs.google.material)
    api(libs.androidx.vectordrawable)
    api(libs.androidx.vectordrawable.animated)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.swiperefreshlayout)
    api(libs.androidx.work.runtime)
    api(libs.easypermissions)
    api(libs.androidx.core.ktx)

    // image
    api(libs.photoview)
    api(libs.coil)
    // parser
    api(libs.gson)

    // view
    api(libs.richtext)
    api(libs.multitype)
    api(libs.shimmerlayout)
    api(libs.lottie)

    // logger
    api(libs.timber)
    // view model
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.livedata.ktx)
    api(libs.androidx.lifecycle.common.java8)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    // guava
    api(libs.guava)
    // jsoup
    api(libs.jsoup)
    api(libs.okhttp)
    api(libs.persistentcookiejar)
    // database
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    // event-bus
    api(libs.eventbus)

    // kotlin
    api(kotlin("stdlib-jdk8"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
}