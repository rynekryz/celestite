plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.rynekryz.celestite"
    compileSdk = 37
    androidResources {
        localeFilters += "en"
    }
    defaultConfig {
        applicationId = "com.rynekryz.celestite"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-beta"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}
androidComponents {
    onVariants { variant ->
        val versionName = variant.outputs.first().versionName.get()
        variant.outputs.forEach { output ->
            val abiFilter = output.filters.find {
                it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI
            }?.identifier
            val abiName = when (abiFilter) {
                "arm64-v8a" -> "arm64"
                "armeabi-v7a" -> "armv7"
                "x86_64" -> "x86_64"
                else -> abiFilter ?: "universal"
            }
            val buildTypeName = variant.buildType ?: "debug"
            output.outputFileName.set("celestite-$versionName-$abiName-$buildTypeName.apk")
        }
    }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:recorder"))
    implementation(project(":feature:gallery"))
    implementation(project(":feature:settings"))
    implementation(project(":service"))
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.5.0-alpha25")
}
