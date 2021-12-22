val ktorVersion = "1.6.5"
val serializationVersion = "1.3.1"
val coroutineVersion = "1.5.2"
val sqlDelightVersion = "1.5.2"

group = "io.datax"
version = "1.0-SNAPSHOT"

buildscript {

    repositories {
        mavenCentral()
    }

    dependencies {
        val kotlinVersion = "1.6.0"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}

plugins {
    val kotlinVersion = "1.6.0"
    kotlin("multiplatform")
    kotlin("plugin.serialization") version kotlinVersion
    id("com.squareup.sqldelight")
    kotlin("native.cocoapods")
    id("com.android.library")
}

kotlin {
    android()
    ios()

    cocoapods {

        ios.deploymentTarget = "14.0"

        summary = "Shared module for Spotify Recommendation Lab"
        homepage = "datax.io"

        pod("AppAuth") { version = "~> 1.4.0" }

        podfile = project.file("../ios/Podfile")

        // Maps custom Xcode configuration to NativeBuildType
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] =
            org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] =
            org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-auth:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("com.squareup.sqldelight:runtime:$sqlDelightVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("com.google.android.material:material:1.4.0")
                implementation("io.ktor:ktor-client-android:$ktorVersion")
                implementation("com.squareup.okhttp3:okhttp:4.9.3")
                implementation("com.squareup.sqldelight:android-driver:$sqlDelightVersion")
                implementation("net.openid:appauth:0.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.1")
            }
        }
        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-ios:$ktorVersion")
                implementation("com.squareup.sqldelight:native-driver:$sqlDelightVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion-native-mt") {
                    version {
                        strictly("$coroutineVersion-native-mt")
                    }
                }
            }
        }
        val iosTest by getting
    }
}

android {
    compileSdk = 29
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.CInteropProcess::class.java) {
    settings.compilerOpts("-DNS_FORMAT_ARGUMENT(A)=")
}

sqldelight {
    database("MainDatabase") {
        packageName = "io.datax.shared.repo"
        sourceFolders = listOf("sqldelight")
        verifyMigrations = true
    }
    linkSqlite = true
}
