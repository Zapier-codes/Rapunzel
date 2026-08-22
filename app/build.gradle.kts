import java.util.Properties

val gitCommitSha = providers.exec {
    commandLine("git", "rev-parse", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.get().trim().takeIf {
    it.matches(Regex("[0-9a-fA-F]{40}"))
} ?: "unknown"

// ============================================================================
// RAPUNZEL DYNAMIC CONFIGURATION LOADER
// Reads from gradle.properties (root) or local.properties (dev overrides).
// CI injects secrets into gradle.properties before build.
// ============================================================================

fun loadConfigProperty(key: String, defaultValue: String = ""): String {
    // 1. Check local.properties first (dev overrides, never committed)
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        val localProps = Properties().apply { load(localPropsFile.inputStream()) }
        localProps.getProperty(key)?.let { return it }
    }
    // 2. Fall back to gradle.properties (committed defaults + CI-injected values)
    val gradlePropsFile = rootProject.file("gradle.properties")
    if (gradlePropsFile.exists()) {
        val gradleProps = Properties().apply { load(gradlePropsFile.inputStream()) }
        gradleProps.getProperty(key)?.let { return it }
    }
    return defaultValue
}

val rapunzelAppName = loadConfigProperty("rapunzel.app.name", "Rapunzel")
val rapunzelAppPackage = loadConfigProperty("rapunzel.app.package", "io.aatricks.novelscraper")
val rapunzelAppVersion = loadConfigProperty("rapunzel.app.version", "1.0.0")
val rapunzelVersionCode = loadConfigProperty("rapunzel.app.versionCode", "100").toIntOrNull() ?: 100
val rapunzelSupabaseUrl = loadConfigProperty("rapunzel.supabase.url", "")
val rapunzelSupabaseAnon = loadConfigProperty("rapunzel.supabase.anon", "")
val rapunzelPawnsKey = loadConfigProperty("rapunzel.pawns.apiKey", "")
val rapunzelWattpadClient = loadConfigProperty("rapunzel.wattpad.clientId", "")
val rapunzelWattpadBase = loadConfigProperty("rapunzel.wattpad.baseUrl", "https://www.wattpad.com")
val rapunzelRoyalRoadBase = loadConfigProperty("rapunzel.royalroad.baseUrl", "https://www.royalroad.com")
val rapunzelInkittBase = loadConfigProperty("rapunzel.inkitt.baseUrl", "https://www.inkitt.com")
val rapunzelFeatPawns = loadConfigProperty("rapunzel.feat.pawns", "false") == "true"
val rapunzelFeatSupabase = loadConfigProperty("rapunzel.feat.supabase", "false") == "true"
val rapunzelFeatWattpad = loadConfigProperty("rapunzel.feat.wattpad", "false") == "true"
val rapunzelFeatRoyalRoad = loadConfigProperty("rapunzel.feat.royalroad", "false") == "true"
val rapunzelFeatInkitt = loadConfigProperty("rapunzel.feat.inkitt", "false") == "true"
val rapunzelFeatAiRag = loadConfigProperty("rapunzel.feat.ai.rag", "false") == "true"
val rapunzelGithubOwner = loadConfigProperty("rapunzel.github.owner", "Zapier-codes")
val rapunzelGithubRepo = loadConfigProperty("rapunzel.github.repo", "Rapunzel")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.androidx.baselineprofile)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    source.setFrom("src/main/java", "src/standard/java", "src/ai/java")
    baseline = file("detekt-baseline.xml")
    parallel = true
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    autoCorrect = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
    jvmTarget = "17"
}

android {
    namespace = "io.aatricks.easyreader"
    compileSdk = 37

    defaultConfig {
        // Dynamic application ID from config. Legacy default preserves existing installs.
        applicationId = rapunzelAppPackage
        minSdk = 30
        targetSdk = 34
        versionCode = rapunzelVersionCode
        versionName = rapunzelAppVersion

        // Git commit SHA for crash reporting and diagnostics
        buildConfigField("String", "GIT_COMMIT_SHA", "\"$gitCommitSha\"")
        buildConfigField("String", "PAWNS_API_KEY", ""${System.getenv("PAWNS_API_KEY") ?: ""}"")

        // ============================================================================
        // DYNAMIC BUILD CONFIG FIELDS — Single source of truth for runtime config
        // ============================================================================
        buildConfigField("String", "APP_NAME", "\"$rapunzelAppName\"")
        buildConfigField("String", "APP_PACKAGE", "\"$rapunzelAppPackage\"")
        buildConfigField("String", "APP_VERSION", "\"$rapunzelAppVersion\"")
        buildConfigField("int", "APP_VERSION_CODE", "$rapunzelVersionCode")
        buildConfigField("String", "SUPABASE_URL", "\"$rapunzelSupabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$rapunzelSupabaseAnon\"")
        buildConfigField("String", "PAWNS_API_KEY", "\"$rapunzelPawnsKey\"")
        buildConfigField("String", "WATTPAD_CLIENT_ID", "\"$rapunzelWattpadClient\"")
        buildConfigField("String", "WATTPAD_API_BASE", "\"$rapunzelWattpadBase\"")
        buildConfigField("String", "ROYALROAD_BASE_URL", "\"$rapunzelRoyalRoadBase\"")
        buildConfigField("String", "INKITT_BASE_URL", "\"$rapunzelInkittBase\"")
        buildConfigField("boolean", "FEATURE_PAWNS", "$rapunzelFeatPawns")
        buildConfigField("boolean", "FEATURE_SUPABASE", "$rapunzelFeatSupabase")
        buildConfigField("boolean", "FEATURE_WATTPAD", "$rapunzelFeatWattpad")
        buildConfigField("boolean", "FEATURE_ROYALROAD", "$rapunzelFeatRoyalRoad")
        buildConfigField("boolean", "FEATURE_INKITT", "$rapunzelFeatInkitt")
        buildConfigField("boolean", "FEATURE_AI_RAG", "$rapunzelFeatAiRag")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"$rapunzelGithubOwner\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"$rapunzelGithubRepo\"")

        // Dynamic app name in resources so manifest and system UI pick it up automatically
        resValue("string", "app_name", rapunzelAppName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties().apply {
                    load(keystorePropertiesFile.inputStream())
                }
                storeFile = properties.getProperty("storeFile")?.let { file(it) }
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            } else {
                // If the file is missing, we don't set the properties.
                // The build will fail only when release flavor tasks are called
                // (for example, assembleStandardRelease or assembleAiRelease),
                // which is the desired behavior for PRs that shouldn't build release.
                println("Warning: keystore.properties not found. Release builds will fail to sign.")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks.add("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
        }
        debug {
            isMinifyEnabled = false
        }
    }

    flavorDimensions.add("version")
    productFlavors {
        create("standard") {
            dimension = "version"
            isDefault = true
        }
        create("ai") {
            dimension = "version"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
        manifestPlaceholders = true
    }
    sourceSets {
        getByName("standard") {
            java.directories.add("src/standard/java")
        }
        getByName("ai") {
            java.directories.add("src/ai/java")
        }
        getByName("debug") {
            assets.directories.add("schemas")
        }
        getByName("test") {
            java.directories.add("src/test/java")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                showStandardStreams = true
            }
        }
    }

    lint {
        // Pin the current set of lint findings so new regressions are visible in CI.
        // Regenerate with: ./gradlew :app:updateLintBaseline
        baseline = file("lint-baseline.xml")
        // Keep the build green when only existing baselined findings remain.
        checkReleaseBuilds = true
        abortOnError = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    packaging {
        resources {
            excludes.add("org/bouncycastle/pqc/crypto/**/*.properties")
            excludes.add("com/itextpdf/io/font/cmap/*")
            excludes.add("com/itextpdf/hyph/*")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    baselineProfile(project(":benchmark"))
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Hilt 2.60's generated code references com.google.errorprone.annotations (now compileOnly in
    // Dagger), so the generated-Java compile needs these annotations on the classpath.
    compileOnly("com.google.errorprone:error_prone_annotations:2.50.0")
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)

    // Navigation
    implementation(libs.navigation.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // llmedge AI Library
    "aiImplementation"(libs.llmedge)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Web Scraping - JSoup
    implementation(libs.jsoup)

    // Image Loading - Coil 3
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // PDF Parsing - iText7
    implementation(libs.itext7.core) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.bouncycastle.bcprov.jdk15to18)
    implementation(libs.bouncycastle.bcpkix.jdk15to18)
    implementation(libs.bouncycastle.bcutil.jdk15to18)

    // Networking - OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    // Ktor's OkHttp engine pulls okhttp-sse in transitively at an older version than the rest
    // of the okhttp family; declaring it explicitly forces it to resolve at the same version so
    // its internals (e.g. RealEventSource) stay binary-compatible with okhttp itself.
    implementation(libs.okhttp.sse)
    // Pawns SDK
    implementation("app.pawns:sdk:1.0.+")
    
    // Retrofit for REST APIs (Wattpad, Inkitt)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
