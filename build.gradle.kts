// AGP 9 内置 Kotlin，自带 KGP 2.2.10。要用更高版本的 Kotlin/KSP 只能在这里用
// buildscript classpath 覆盖 —— 因此 catalog 里 parcelize / ksp 不能再带版本号。
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}