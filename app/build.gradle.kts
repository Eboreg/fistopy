import org.gradle.configurationcache.extensions.capitalized
import java.io.FileInputStream
import java.util.Properties

val currentVersionCode = 20
val currentVersionName = "0.9.0"
val keystoreProperties = Properties()
val secretsProperties = Properties()

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sentry)
    id("kotlin-parcelize")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "us.huseli.fistopy"
    compileSdk = 35

    applicationVariants.all {
        outputs.all {
            val taskSuffix = name
                .capitalized()
                .replace(Regex("[-_](\\p{L})")) { it.groupValues[1].uppercase() }
            val assembleTaskName = "assemble$taskSuffix"
            val bundleTaskName = "bundle$taskSuffix"

            val archiveTask = tasks.create(name = "archive$taskSuffix") {
                actions.add(
                    Action {
                        val inDir = outputFile.parentFile
                        val outDir =
                            File("${inDir?.parentFile?.path}/$name-$versionName").apply { mkdirs() }

                        inDir?.listFiles()?.filter { it.isFile && it.name.contains(versionName) }?.forEach { file ->
                            file.copyTo(File(outDir, file.name), overwrite = true)
                        }
                    }
                )
            }

            try {
                tasks[assembleTaskName]?.finalizedBy(archiveTask)
                tasks[bundleTaskName]?.finalizedBy(archiveTask)
            } catch (e: UnknownTaskException) {
                logger.error(e.toString(), e)
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    defaultConfig {
        manifestPlaceholders += mapOf("redirectSchemeName" to "klaatu")
        val discogsApiKey = secretsProperties["discogsApiKey"] as String
        val discogsApiSecret = secretsProperties["discogsApiSecret"] as String
        val spotifyClientId = secretsProperties["spotifyClientId"] as String
        val spotifyClientSecret = secretsProperties["spotifyClientSecret"] as String
        val lastFmApiKey = secretsProperties["lastFmApiKey"] as String
        val lastFmApiSecret = secretsProperties["lastFmApiSecret"] as String

        applicationId = "us.huseli.fistopy"
        minSdk = 26
        targetSdk = 34
        versionCode = currentVersionCode
        versionName = currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        setProperty("archivesBaseName", "fistopy_$versionName")

        buildConfigField("String", "discogsApiKey", "\"$discogsApiKey\"")
        buildConfigField("String", "discogsApiSecret", "\"$discogsApiSecret\"")
        buildConfigField("String", "spotifyClientId", "\"$spotifyClientId\"")
        buildConfigField("String", "spotifyClientSecret", "\"$spotifyClientSecret\"")
        buildConfigField("String", "lastFmApiKey", "\"$lastFmApiKey\"")
        buildConfigField("String", "lastFmApiSecret", "\"$lastFmApiSecret\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            // isRenderscriptDebuggable = true
            applicationIdSuffix = ".debug"
            manifestPlaceholders["hostName"] = "fistopy.debug"
            manifestPlaceholders["redirectHostName"] = "fistopy.debug"
            buildConfigField("String", "hostName", "\"fistopy.debug\"")
        }
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            manifestPlaceholders["hostName"] = "fistopy"
            manifestPlaceholders["redirectHostName"] = "fistopy"
            buildConfigField("String", "hostName", "\"fistopy\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "version"

    productFlavors {
        create("sentry") {
            applicationIdSuffix = ".sentry"
            versionNameSuffix = "-sentry"

            dependencies {
                "sentryImplementation"(libs.sentry)
                "sentryImplementation"(libs.sentry.compose)
            }

            sentry {
                org.set("huselius")
                projectName.set("fistopy")
                // this will upload your source code to Sentry to show it as part of the stack traces
                // disable if you don't want to expose your sources
                includeSourceContext.set(true)
                ignoredFlavors.set(setOf("nosentry"))
            }
        }
        create("nosentry") {
            applicationIdSuffix = ".nosentry"
            versionNameSuffix = "-nosentry"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)

    // Coil for async image loading:
    implementation(libs.coil.compose)

    // Compose:
    implementation(platform(libs.androidx.compose.bom))

    // Compose related:
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Compose tracing for debugging/optimization:
    implementation(libs.androidx.runtime.tracing)

    // FFMPEG:
    implementation(files("ffmpeg-kit.aar"))
    implementation(libs.smart.exception.java)

    // Glance for widget:
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Gson:
    implementation(libs.gson)

    // Hilt:
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Immutable collections:
    implementation(libs.kotlinx.collections.immutable)

    // Levenshtein string distance:
    implementation(libs.commons.text)

    // Lifecycle:
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Material:
    implementation(libs.androidx.material3)
    implementation(libs.material.icons.extended)

    // Media3:
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    // Reorder:
    implementation(libs.reorderable)

    // Room:
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // SimpleStorage for easier file handling:
    implementation(libs.storage)

    // Splashscreen:
    implementation(libs.androidx.core.splashscreen)

    // Theme etc:
    implementation(libs.retain.theme)

    // Track amplitude waveform shit:
    implementation(libs.amplituda)
    implementation(libs.compose.audiowaveform)

    // XStream to parse XML:
    implementation(libs.xstream)
}
