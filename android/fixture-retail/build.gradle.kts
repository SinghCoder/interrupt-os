plugins {
    id("com.android.application")
}

android {
    namespace = "chat.harpinderjots.fixture.market"
    compileSdk = 37
    defaultConfig {
        applicationId = "chat.harpinderjots.fixture.market"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
