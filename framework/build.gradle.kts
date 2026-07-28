@file:Suppress("DEPRECATION")

plugins {
    id("com.android.library")
}

android {
    namespace = "top.easelink.framework"
    compileSdk = 37

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    lint {
        targetSdk = 36
    }
    testOptions {
        targetSdk = 36
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
    api(libs.kotlin.stdlib.jdk8)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
}