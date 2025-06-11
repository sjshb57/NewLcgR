pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://tencent-tds-maven.pkg.coding.net/repository/shiply/repo") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://tencent-tds-maven.pkg.coding.net/repository/shiply/repo") }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "NewLcgR"
include(":app")
include(":framework")