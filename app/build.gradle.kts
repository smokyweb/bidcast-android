import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date

        plugins {
            alias(libs.plugins.androidApplication)
            alias(libs.plugins.kotlin.parcelize)
            alias(libs.plugins.kotlin.android)
            alias(libs.plugins.hiltAndroid)
            alias(libs.plugins.devtools)
        }

fun getAPKName() = "Bid_Swipe_${SimpleDateFormat("dd-MM-yyyy").format(Date())}"


android {
    namespace = "io.bidswipe.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.bidswipe.app"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
            setProperty("archivesBaseName", getAPKName())
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.ExperimentalUnsignedTypes",
                "-Xjvm-default=all"
            )
        }
    }
}



dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    //ANDROID DEPENDENCIES
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.billing.ktx)

    //GOOGLE DEPENDENCIES
    implementation(libs.hilt.android)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    ksp(libs.hilt.compiler)
    implementation(libs.flexbox)

    //KOTLIN DEPENDENCIES
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.stdlib)

    //RETROFIT DEPENDENCIES
    implementation(libs.logging.interceptor)
    implementation(libs.converter.gson)
    implementation(libs.retrofit)

    //GLIDE DEPENDENCIES
    implementation(libs.glide.transformations)
    implementation(libs.glide)
    ksp(libs.compiler)

    //STRIPE DEPENDENCY
    implementation (libs.stripe.android)

    //ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)


    //THIRD PARTY DEPENDENCIES
    implementation(libs.recyclerview.animators)
    implementation(libs.material.calendar.view)
    implementation(libs.android.image.cropper)
    implementation(libs.android.spinKit)
    implementation(libs.permissionx)
    implementation(libs.toasty)
    implementation(libs.decorator)
    implementation(libs.picasso)
    implementation(libs.roundedimageview)
    implementation(libs.cameraview)
    implementation(libs.easyvalidation.core)
    implementation(libs.locale.helper.android)
    implementation(libs.yuanwenhai.html.textview)
    implementation(libs.expandableLayout)


}