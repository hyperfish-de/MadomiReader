plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.madomireader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.madomireader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Core / Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coroutines.android)

    // --- Jetpack Compose (BOM keeps every Compose artifact on one version) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    debugImplementation(libs.androidx.ui.tooling)

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Image loading + local cache ---
    implementation(libs.coil.compose)

    // --- Networking + JSON (Gemini REST) ---
    // okhttp 4.12.0 chỉ dùng để BIÊN DỊCH (Kotlin 2.0.21 đọc được); lúc chạy
    // okhttp 5.4.0 thay thế (xem khối runtimeOnly bên dưới).
    implementation(libs.okhttp)
    implementation(libs.gson)

    // --- Tiện ích mở rộng (Keiyoushi / Tachiyomi extensions) ---
    // Bộ API host `eu.kanade.tachiyomi.*` nằm ngay trong module này.
    // kotlinx.serialization: SManga.memo/SChapter.memo là JsonObject, và host phải
    // đăng ký `Json` + `ProtoBuf` cho injekt (extension lấy qua Injekt.get()).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    // androidx.preference: ConfigurableSource.setupPreferenceScreen(PreferenceScreen).
    implementation(libs.androidx.preference.ktx)
    // injekt: extension gọi Injekt.get() để lấy Application/Json/ProtoBuf.
    implementation(libs.injekt.core)

    // --- Thư viện CHỈ có lúc chạy: các extension Keiyoushi 1.6 được biên dịch dựa
    // trên phiên bản mới hơn (okhttp 5.x, serialization 1.11, jsoup 1.22…). Ta
    // biên dịch với bản cũ nhưng đóng gói bản mới để mã của extension chạy được.
    runtimeOnly(libs.okhttp.runtime)
    runtimeOnly(libs.okhttp.brotli)
    runtimeOnly(libs.okhttp.zstd)
    runtimeOnly(libs.okio.runtime)
    runtimeOnly(libs.jsoup.runtime)
    runtimeOnly(libs.jspecify)
    runtimeOnly(libs.kotlinx.serialization.json.runtime)
    runtimeOnly(libs.kotlinx.serialization.protobuf.runtime)
    runtimeOnly(libs.kotlinx.serialization.json.okio)
    runtimeOnly(libs.zstd.kmp.okio)

    // --- Dịch trên máy (ML Kit, không cần mạng sau khi tải gói ngôn ngữ) ---
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    // --- Persistence (settings + reading history) ---
    implementation(libs.datastore.preferences)

    // --- On-device OCR (ML Kit Text Recognition v2, bundled models) ---
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)

    // --- Tests ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
