plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.programmerq.bruneiazan.shared"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
