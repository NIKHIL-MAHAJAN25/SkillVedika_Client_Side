import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) { // <-- This check is missing
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.nikhil.buyerapp"
    compileSdk = 35
    defaultConfig {
    applicationId = "com.nikhil.buyerapp"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"




        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")?:""}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("SUPABASE_KEY")?:""}\"")
        buildConfigField("String","NEWS_KEY","\"${localProperties.getProperty("NEWS_KEY")?:""}\"")
        buildConfigField("String","GOOGLE_KEY","\"${localProperties.getProperty("GOOGLE_KEY")?:""}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("KEYSTORE_PATH") ?: "")
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding=true
        buildConfig=true
    }
}


dependencies {
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    // 1. PDF & Markdown Tools
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")

    // 2. GOOGLE GEMINI AI (Requires Ktor 2)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // --- THE NETWORK ENGINE (Ktor 2) ---
    // Everything must match version 2.3.12
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")

    // 3. SUPABASE (Downgraded to 2.6.1 to match Ktor 2)
    // Version 3.0.0+ breaks compatibility with Gemini because it forces Ktor 3
    implementation(platform("io.github.jan-tennert.supabase:bom:2.6.1"))
    implementation("io.github.jan-tennert.supabase:storage-kt") // Version comes from BOM
    implementation("io.github.jan-tennert.supabase:gotrue-kt")  // Auth (if you need it)

    // 4. RETROFIT (For News API & Brandfetch)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 5. UI & ANIMATIONS
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.caverock:androidsvg:1.4")
    implementation("com.hbb20:ccp:2.6.1")

    // 6. GLIDE
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // 7. ANDROID LIBS
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")

    implementation(libs.firebase.firestore)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}