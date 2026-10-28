import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun loadProps(name: String): Properties {
    val p = Properties()
    val f = rootProject.file(name)
    if (f.exists()) f.inputStream().use { p.load(it) }
    return p
}

val supabaseProps = loadProps("supabase.properties")
val keystoreProps = loadProps("keystore.properties")
val hasKeystore = keystoreProps.getProperty("storeFile") != null
// -PnoNative=true: Compose er bhitorer ekmatro native lib (libandroidx.graphics.path.so) bundle theke bad dey,
// jate Play Console er "native code, no debug symbols" warning na ashe. Default: lib bad jay (-PnoNative=false dile thake).
// App nije PathIterator/RoundedPolygon use kore na, tai lib ta lage na.
val dropNativeLib = (project.findProperty("noNative") as String?) != "false"

android {
    namespace = "com.tricosoft.booknest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tricosoft.booknest"
        minSdk = 24
        targetSdk = 36
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 7
        versionName = (project.findProperty("versionName") as String?) ?: "7.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"${supabaseProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supabaseProps.getProperty("SUPABASE_KEY", "")}\"")
        buildConfigField("String", "PRIVACY_URL", "\"https://app-privacy-center.blogspot.com/2026/08/booknest.html\"")
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Compose er bhitor chhoto native lib ache; symbol table dile Play Console er "no debug symbols" warning jay
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            if (dropNativeLib) {
                excludes += "**/libandroidx.graphics.path.so"
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
