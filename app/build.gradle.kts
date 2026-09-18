plugins {
    id("com.android.application") version "8.2.0"
}

android {
    namespace = "com.apollo.forged"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.apollo.forged"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
