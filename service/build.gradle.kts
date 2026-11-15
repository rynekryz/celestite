plugins {
    id("com.android.library")
}
android {
    namespace = "com.rynekryz.celestite.service"
    compileSdk = 37
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.core:core:1.17.0")
}
