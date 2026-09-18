plugins {
    id("com.android.application") version "8.2.0"
}

android {
    namespace = "com.apollo.missioncontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.apollo.missioncontrol"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
