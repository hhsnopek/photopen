import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// Signing secrets never live in the repository. CI supplies them as environment
// variables; a local build can keep them in local.properties, which git ignores.
val localSecrets =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }

fun secret(
    name: String,
    property: String,
): String? = System.getenv(name) ?: localSecrets.getProperty("photopen.$property")

android {
    namespace = "dev.hhsnopek.photopen"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.hhsnopek.photopen"
        // MediaStore, Media3 and Compose all run comfortably on 26; the
        // READ_MEDIA_* permissions are handled with a runtime branch rather
        // than by raising the floor.
        minSdk = 26
        targetSdk = 35
        versionCode = 10000
        versionName = "1.0.0"

        resourceConfigurations += listOf("en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // `make keystore` writes this file; CI decodes it from a secret.
            val keystoreFile = file("keystore/release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = secret("KEYSTORE_PASSWORD", "keystorePassword")
                    ?: error("KEYSTORE_PASSWORD is not set (environment or local.properties)")
                keyAlias =
                    System.getenv("KEY_ALIAS")
                        ?: localSecrets.getProperty("photopen.keyAlias")
                        ?: "photopen"
                keyPassword = secret("KEY_PASSWORD", "keyPassword")
                    ?: error("KEY_PASSWORD is not set (environment or local.properties)")
            }
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

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        allWarningsAsErrors = true
    }

    buildTypes {
        debug {
            // Same package as release, so `make install` replaces an Obtainium
            // build only when both are signed with the same key.
        }
        release {
            // Sideloaded and updated in place by Obtainium, so it must be
            // signed with a stable release key. When the keystore is missing
            // (a fresh clone) the debug key keeps local builds installable;
            // such an APK cannot update one built elsewhere.
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig =
                if (releaseSigning?.storeFile?.exists() == true) {
                    releaseSigning
                } else {
                    signingConfigs.getByName("debug")
                }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true

        disable +=
            listOf(
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "OldTargetApi",
                "ObsoleteSdkInt",
                "MissingTranslation",
            )
    }

    packaging {
        resources.excludes +=
            setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
            )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.collections.immutable)

    // Video playback in the player, and the surface/controls around it.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Image decoding, thumbnails, and video frame thumbnails.
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

dependencies {
    // Compose-specific detekt rules (ModifierMissing, RememberMissing, ...).
    detektPlugins(libs.detekt.compose.rules)
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}
