plugins {
    id("com.android.library")
}
android {
    namespace = "com.rynekryz.celestite.core.datastore"
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
    api(project(":core:model"))
    api("androidx.datastore:datastore-preferences:1.2.1")
}
