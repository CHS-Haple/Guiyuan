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
val miuixSourceDir = providers.gradleProperty("gy.os4.miuixDir").orNull?.takeIf(String::isNotBlank)
val miuixSourceRevision = providers.gradleProperty("gy.os4.miuixRevision").orNull?.takeIf(String::isNotBlank)
require(miuixSourceRevision == null || (miuixSourceDir != null && Regex("[0-9a-f]{40}").matches(miuixSourceRevision))) {
    "MIUIX source revision must be a full lowercase commit SHA paired with a source directory"
}
// Do not report the published dependency baseline as the version of a source-built candidate.
val miuixDisplayVersion =
    if (miuixSourceDir == null) miuixVersion
    else "source ${miuixSourceRevision?.take(8) ?: "(revision unspecified)"}"
val miuixDisplayRevision =
    if (miuixSourceDir == null) miuixRevision
    else miuixSourceRevision ?: "unverified local source"
val libxposedVersion = "102.0.0"
val coreVersion = "1.19.1"
val activityComposeVersion = "1.13.0"
val navigationEventComposeVersion = "1.2.0"
val dataStorePreferencesVersion = "1.3.0"
val kotlinxSerializationCoreVersion = "1.12.0"
val junitVersion = "4.13.2"
val gradleVersion = gradle.gradleVersion
val os4DebugSuffix = providers.gradleProperty("gy.os4.debugSuffix").orNull
val os4DialogSurfaces = providers.gradleProperty("gy.os4.dialogSurfaces").orNull == "true"
// The marker exists only on the protected IT branch and is blocked from public PR targets.
val isItChannel = rootProject.file("it-only/RESTRICT_PROJECT_LINK").isFile
val projectLinkEnabled = !isItChannel
val itVersionSuffix = providers.gradleProperty("combinedStatus.itVersionSuffix").orNull
require(itVersionSuffix == null || (isItChannel && itVersionSuffix == "-IT")) {
    "IT version suffix requires the protected IT channel marker and must be -IT"
}
if (os4DialogSurfaces) {
    require(providers.gradleProperty("gy.os4.miuixDir").isPresent) {
        "GlassDialog requires the same-revision MIUIX source graph (-Pgy.os4.miuixDir)"
    }
}

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
        buildConfigField("String", "MIUIX_VERSION", "\"$miuixDisplayVersion\"")
        buildConfigField("String", "MIUIX_REVISION", "\"$miuixDisplayRevision\"")
        buildConfigField("String", "LIBXPOSED_VERSION", "\"$libxposedVersion\"")
        buildConfigField("String", "ANDROIDX_CORE_VERSION", "\"$coreVersion\"")
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

    // Match the dialog implementation to MIUIX's compiled DialogSurfaceScope ABI.
    sourceSets.getByName("main").kotlin.directories.add(
        if (os4DialogSurfaces) "src/os4Upstream/kotlin" else "src/os4Published/kotlin",
    )

    buildTypes {
        debug {
            if (itVersionSuffix != null) versionNameSuffix = itVersionSuffix
            if (os4DebugSuffix != null) {
                require(Regex("""\.[a-z][a-z0-9_]*""").matches(os4DebugSuffix)) {
                    "gy.os4.debugSuffix must be one simple lowercase application ID segment"
                }
                applicationIdSuffix = os4DebugSuffix
            }
            buildConfigField("String", "BUILD_CHANNEL", "\"debug\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "true")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "true")
            buildConfigField("boolean", "REPO_LINK_ENABLED", projectLinkEnabled.toString())
            if (hapleSigningEnabled) {
                signingConfig = signingConfigs.getByName("haple")
            }
        }
        release {
            buildConfigField("String", "BUILD_CHANNEL", "\"release\"")
            buildConfigField("boolean", "DEVELOPMENT_PROBES", "false")
            buildConfigField("boolean", "RUNTIME_DIAGNOSTICS", "false")
            buildConfigField("boolean", "REPO_LINK_ENABLED", projectLinkEnabled.toString())
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
            if (itVersionSuffix != null) versionNameSuffix = itVersionSuffix
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

    implementation("androidx.core:core:$coreVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")
    implementation("androidx.navigationevent:navigationevent-compose:$navigationEventComposeVersion")
    implementation("androidx.datastore:datastore-preferences:$dataStorePreferencesVersion")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:$miuixVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationCoreVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationCoreVersion")
}
