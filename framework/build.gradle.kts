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

    // 只声明本模块源码真正 import 到的东西。
    // 之前这里用 api 把 Room / WorkManager / Jsoup / OkHttp / EventBus / Gson /
    // Guava / PhotoView / MultiType / Lottie 全导出了一遍，而 framework 一个都没用到。
    api(libs.androidx.appcompat)          // AppCompatActivity
    api(libs.androidx.core.ktx)           // ViewCompat, graphics 扩展
    api(libs.fragment.ktx)                // Fragment, DialogFragment
    api(libs.androidx.recyclerview)       // linkagerv
    api(libs.androidx.swiperefreshlayout) // ScrollChildSwipeRefreshLayout
    api(libs.google.material)             // HtmlTextView 继承 MaterialTextView
    api(libs.androidx.annotation)
    api(libs.coil)                        // HtmlCoilImageGetter
    api(libs.timber)

    api(libs.androidx.lifecycle.viewmodel.ktx)

    api(libs.kotlin.stdlib.jdk8)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
}
