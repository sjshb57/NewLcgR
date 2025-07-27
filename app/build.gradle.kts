plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.parcelize)
}

val versionMajor = 2
val versionMinor = 0
val versionPatch = 2

android {
    namespace = "top.easelink.lcg"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.easelink.lcg"
        minSdk = 23
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

        buildConfigField("String", "DB_NAME", "\"articles.db\"")
        buildConfigField("String", "SHIPLY_APP_ID",  "\"" + System.getenv("SHIPLY_APP_ID") + "\"")
        buildConfigField("String", "SHIPLY_APP_KEY", "\"" + System.getenv("SHIPLY_APP_KEY") + "\"")

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
        resConfigs("zh-rCN","zh-rTW")
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PWD")
            storeFile = file("../keystore.jks")
            storePassword = System.getenv("RELEASE_KEY_STORE_PWD")
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        //    isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
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

    packaging {
        resources.excludes += "**/libc++_shared.so"
        resources.excludes += "**/libmmkv.so"
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/*android*")
            excludes.add("META-INF/*kotlin*")
            excludes.add("okhttp3/**")
            excludes.add("kotlin/**")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/README.md")
            excludes.add("META-INF/services/**")
            excludes.add("META-INF/CHANGES")
        }
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.base)
    implementation(project(":framework"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Debug
    debugImplementation(libs.debug.db)

    // Shiply
    implementation(libs.shiply.upgrade)
    implementation(libs.shiply.upgrade.ui)

    // AndroidX
    implementation(libs.androidx.annotation)
    implementation(libs.google.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.androidx.tracing)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // UI
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.vectordrawable.animated)

    // Lifecycle
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Third-party
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.easypermissions)
    implementation(libs.eventbus)
    implementation(libs.gson)
    implementation(libs.guava)
    implementation(libs.jsoup)
    implementation(libs.lottie)
    implementation(libs.multitype)
    implementation(libs.okhttp)
    implementation(libs.persistentcookiejar)
    implementation(libs.photoview)
    implementation(libs.richtext)
    implementation(libs.shimmerlayout)
    implementation(libs.timber)
    implementation(libs.kotlin.stdlib.jdk8)
    testImplementation(libs.junit)
}