plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.parcelize)
}

val versionMajor = 1
val versionMinor = 9
val versionPatch = 9

android {
    namespace = "top.easelink.lcg"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "top.easelink.lcg"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
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
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
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
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.base)
    implementation(project(":framework"))
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    debugImplementation(libs.debug.db)
    implementation(libs.shiply.upgrade)
    implementation(libs.shiply.upgrade.ui)
    implementation(libs.androidx.annotation)

    implementation(libs.google.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.fragment.ktx)
}