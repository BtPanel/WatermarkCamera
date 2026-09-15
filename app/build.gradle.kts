import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val amapApiKey = (project.findProperty("AMAP_API_KEY") as String?) ?: ""
val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.watermarkcamera.studio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.watermarkcamera.studio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
        buildConfigField("String", "AMAP_API_KEY", "\"$amapApiKey\"")
    }

    val releaseSigningConfig = if (releaseSigningPropertiesFile.isFile) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
            storePassword = releaseSigningProperties.getProperty("storePassword")
            keyAlias = releaseSigningProperties.getProperty("keyAlias")
            keyPassword = releaseSigningProperties.getProperty("keyPassword")
        }
    } else null

    buildTypes {
        getByName("release") {
            releaseSigningConfig?.let { signingConfig = it }
            isDebuggable = false
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")

    implementation("androidx.compose.ui:ui:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")

    val cameraX = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.amap.api:3dmap-location-search:11.2.100_loc11.2.100_sea9.8.1")

    testImplementation("junit:junit:4.13.2")
}
