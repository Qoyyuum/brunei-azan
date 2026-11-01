plugins {
    alias(libs.plugins.android.application)
}

android {
    // Watch Face Format packages are resource-only (hasCode="false").
    enableKotlin = false
    namespace = "it.anak.bruneiazan.watchface"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "it.anak.bruneiazan.watchface"
        minSdk = 33
        targetSdk = 37
        // Offset convention: mobile=1, wear=10000+, watchface=20000+ — Play
        // requires unique version codes across all artifacts in one record.
        versionCode = 20001
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Minify strips the generated R class — a Watch Face Format
            // package must not contain any dex at minSdk >= 33. Resources
            // must not be shrunk: watchface.xml is read by the system
            // renderer, not by app code.
            isMinifyEnabled = true
            isShrinkResources = false
        }
    }
    buildFeatures {
        buildConfig = false
    }
}
