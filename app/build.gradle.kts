plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val appVersionName = providers.gradleProperty("combinedStatus.versionName").get()
val appVersionCode = providers.gradleProperty("combinedStatus.versionCode").get().toInt()
val buildId = providers.gradleProperty("combinedStatus.buildId").get()
val miuixVersion = providers.gradleProperty("miuix.version").get()
val miuixRevision = providers.gradleProperty("miuix.revision").get()
val backdropVersion = providers.gradleProperty("backdrop.version").get()
val shapesVersion = providers.gradleProperty("shapes.version").get()
val libxposedVersion = "102.0.0"
val activityComposeVersion = "1.13.0"
val navigationEventComposeVersion = "1.2.0"
val dataStorePreferencesVersion = "1.2.1"
val kotlinxSerializationCoreVersion = "1.11.0"
val junitVersion = "4.13.2"
val gradleVersion = gradle.gradleVersion

val hapleKeystorePath = providers.environmentVariable("HAPLE_KEYSTORE_PATH").orNull
val hapleKeystorePassword = providers.environmentVariable("HAPLE_KEYSTORE_PASSWORD").orNull
val hapleKeyAlias = providers.environmentVariable("HAPLE_KEY_ALIAS").orNull
val hapleSigningEnabled =
    !hapleKeystorePath.isNullOrBlank() &&
        !hapleKeystorePassword.isNullOrBlank() &&
        !hapleKeyAlias.isNullOrBlank() &&
        file(hapleKeystorePath).isFile

@Suppress("UnstableApiUsage")
android {
    namespace = "com.chaners.guiyuan"
    buildToolsVersion = "37.0.0"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.chaners.guiyuan"
        minSdk = 33
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "BUILD_ID", "\"$buildId\"")
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixVersion\"")
        buildConfigField("String", "MIUIX_REVISION", "\"$miuixRevision\"")
        buildConfigField("String", "BACKDROP_VERSION", "\"$backdropVersion\"")
        buildConfigField("String", "SHAPES_VERSION", "\"$shapesVersion\"")
        buildConfigField("String", "LIBXPOSED_VERSION", "\"$libxposedVersion\"")
        buildConfigField("String", "ACTIVITY_COMPOSE_VERSION", "\"$activityComposeVersion\"")
        buildConfigField("String", "NAVIGATION_EVENT_COMPOSE_VERSION", "\"$navigationEventComposeVersion\"")
        buildConfigField("String", "DATASTORE_PREFERENCES_VERSION", "\"$dataStorePreferencesVersion\"")
        buildConfigField("String", "KOTLINX_SERIALIZATION_CORE_VERSION", "\"$kotlinxSerializationCoreVersion\"")
        buildConfigField("String", "JUNIT_VERSION", "\"$junitVersion\"")
        buildConfigField("String", "GRADLE_VERSION", "\"$gradleVersion\"")
    }

    signingConfigs {
        if (hapleSigningEnabled) {
            create("haple") {
                storeFile = file(requireNotNull(hapleKeystorePath))
                storePassword = hapleKeystorePassword
                keyAlias = hapleKeyAlias
                keyPassword = hapleKeystorePassword

                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BUILD_CHANNEL", "\"debug\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "true")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
            buildConfigField("boolean", "REPO_LINK_ENABLED", "true")
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
        }
        release {
            buildConfigField("String", "BUILD_CHANNEL", "\"release\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "false")
            buildConfigField("boolean", "REPO_LINK_ENABLED", "true")
            isMinifyEnabled = true
            isShrinkResources = true
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("canary") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            buildConfigField("String", "BUILD_CHANNEL", "\"canary\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
        }
        create("internal") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            versionNameSuffix = "-internal"
            buildConfigField("String", "BUILD_CHANNEL", "\"internal\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
            buildConfigField("boolean", "REPO_LINK_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            // Modern Xposed metadata is loaded directly by the framework and must survive
            // optimized non-debug packaging even when dependency graphs change.
            merges += "META-INF/xposed/**"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:$libxposedVersion")
    implementation("io.github.libxposed:service:$libxposedVersion")

    testImplementation("junit:junit:$junitVersion")

    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.navigationevent:navigationevent-compose:$navigationEventComposeVersion")
    implementation("androidx.datastore:datastore-preferences:$dataStorePreferencesVersion")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("io.github.kyant0:backdrop:$backdropVersion")
    implementation("io.github.kyant0:shapes:$shapesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationCoreVersion")
}
