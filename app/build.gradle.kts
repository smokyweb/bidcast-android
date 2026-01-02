import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date

plugins {
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.kotlin.parcelize)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.hiltAndroid)
	alias(libs.plugins.devtools)
	alias(libs.plugins.google.gms.google.services)
	alias(libs.plugins.crashlytics)
}

fun getAPKName() = "bidswipe_debug_${SimpleDateFormat("dd-MM-yyyy").format(Date())}"

android {
	namespace = "io.bidswipe.app"
	compileSdk = 36

	defaultConfig {
		applicationId = "io.bidswipe.app"
		minSdk = 26
		targetSdk = 36
		versionCode = 1
		versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

	    buildConfigField("String", "STRIPE_PK", "\"pk_test_51SjiEtQzmy9jx34KXrnMJIwqLx5IfCN69oZsNCptlyBfChq7NrJVc8OjS5q16nvnuobjjp3Run8icoXQHn0D9eVG00nJyhk9zM\"")

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

	packaging {
		jniLibs.useLegacyPackaging = false

		jniLibs.keepDebugSymbols.addAll(listOf(
			"**/libagora-fdkaac.so",
			"**/libagora-ffmpeg.so",
			"**/libagora-rtc-sdk.so",
			"**/libagora-soundtouch.so",
			"**/libandroidx.graphics.path.so",
			"**/libaosl.so",
			"**/libdatastore_shared_counter.so"
		))

		jniLibs.excludes += setOf(
			// armeabi-v7a
			"lib/armeabi-v7a/libagora_lip_sync_extension.so",
			"lib/armeabi-v7a/libagora_spatial_audio_extension.so",
			"lib/armeabi-v7a/libagora_clear_vision_extension.so",
			"lib/armeabi-v7a/libagora_segmentation_extension.so",
			"lib/armeabi-v7a/libagora_face_capture_extension.so",
			"lib/armeabi-v7a/libagora_content_inspect_extension.so",
			"lib/armeabi-v7a/libagora_audio_beauty_extension.so",
			"lib/armeabi-v7a/libagora_video_av1_encoder_extension.so",
			"lib/armeabi-v7a/libvideo_enc.so",
			"lib/armeabi-v7a/libvideo_dec.so",
			"lib/armeabi-v7a/libagora_video_quality_analyzer_extension.so",
			"lib/armeabi-v7a/libagora_video_av1_decoder_extension.so",
			"lib/armeabi-v7a/libagora_face_detection_extension.so",
			"lib/armeabi-v7a/libagora_ai_echo_cancellation_extension.so",
			"lib/armeabi-v7a/libagora_ai_echo_cancellation_ll_extension.so",
			"lib/armeabi-v7a/libagora_video_encoder_extension.so",
			"lib/armeabi-v7a/libagora_video_decoder_extension.so",
			"lib/armeabi-v7a/libagora_ai_noise_suppression_extension.so",
			"lib/armeabi-v7a/libagora_ai_noise_suppression_ll_extension.so",
			"lib/armeabi-v7a/libagora_screen_capture_extension.so",

			// arm64-v8a
			"lib/arm64-v8a/libagora_lip_sync_extension.so",
			"lib/arm64-v8a/libagora_spatial_audio_extension.so",
			"lib/arm64-v8a/libagora_clear_vision_extension.so",
			"lib/arm64-v8a/libagora_segmentation_extension.so",
			"lib/arm64-v8a/libagora_face_capture_extension.so",
			"lib/arm64-v8a/libagora_content_inspect_extension.so",
			"lib/arm64-v8a/libagora_audio_beauty_extension.so",
			"lib/arm64-v8a/libagora_video_av1_encoder_extension.so",
			"lib/arm64-v8a/libvideo_enc.so",
			"lib/arm64-v8a/libvideo_dec.so",
			"lib/arm64-v8a/libagora_video_quality_analyzer_extension.so",
			"lib/arm64-v8a/libagora_video_av1_decoder_extension.so",
			"lib/arm64-v8a/libagora_face_detection_extension.so",
			"lib/arm64-v8a/libagora_ai_echo_cancellation_extension.so",
			"lib/arm64-v8a/libagora_ai_echo_cancellation_ll_extension.so",
			"lib/arm64-v8a/libagora_video_encoder_extension.so",
			"lib/arm64-v8a/libagora_video_decoder_extension.so",
			"lib/arm64-v8a/libagora_ai_noise_suppression_extension.so",
			"lib/arm64-v8a/libagora_ai_noise_suppression_ll_extension.so",
			"lib/arm64-v8a/libagora_screen_capture_extension.so",

			// x86
			"lib/x86/libagora_lip_sync_extension.so",
			"lib/x86/libagora_spatial_audio_extension.so",
			"lib/x86/libagora_clear_vision_extension.so",
			"lib/x86/libagora_segmentation_extension.so",
			"lib/x86/libagora_face_capture_extension.so",
			"lib/x86/libagora_content_inspect_extension.so",
			"lib/x86/libagora_audio_beauty_extension.so",
			"lib/x86/libagora_video_av1_encoder_extension.so",
			"lib/x86/libvideo_enc.so",
			"lib/x86/libvideo_dec.so",
			"lib/x86/libagora_video_quality_analyzer_extension.so",
			"lib/x86/libagora_video_av1_decoder_extension.so",
			"lib/x86/libagora_face_detection_extension.so",
			"lib/x86/libagora_ai_echo_cancellation_extension.so",
			"lib/x86/libagora_ai_echo_cancellation_ll_extension.so",
			"lib/x86/libagora_video_encoder_extension.so",
			"lib/x86/libagora_video_decoder_extension.so",
			"lib/x86/libagora_ai_noise_suppression_extension.so",
			"lib/x86/libagora_ai_noise_suppression_ll_extension.so",
			"lib/x86/libagora_screen_capture_extension.so",

			// x86_64
			"lib/x86_64/libagora_lip_sync_extension.so",
			"lib/x86_64/libagora_spatial_audio_extension.so",
			"lib/x86_64/libagora_clear_vision_extension.so",
			"lib/x86_64/libagora_segmentation_extension.so",
			"lib/x86_64/libagora_face_capture_extension.so",
			"lib/x86_64/libagora_content_inspect_extension.so",
			"lib/x86_64/libagora_audio_beauty_extension.so",
			"lib/x86_64/libagora_video_av1_encoder_extension.so",
			"lib/x86_64/libvideo_enc.so",
			"lib/x86_64/libvideo_dec.so",
			"lib/x86_64/libagora_video_quality_analyzer_extension.so",
			"lib/x86_64/libagora_video_av1_decoder_extension.so",
			"lib/x86_64/libagora_face_detection_extension.so",
			"lib/x86_64/libagora_ai_echo_cancellation_extension.so",
			"lib/x86_64/libagora_ai_echo_cancellation_ll_extension.so",
			"lib/x86_64/libagora_video_encoder_extension.so",
			"lib/x86_64/libagora_video_decoder_extension.so",
			"lib/x86_64/libagora_ai_noise_suppression_extension.so",
			"lib/x86_64/libagora_ai_noise_suppression_ll_extension.so",
			"lib/x86_64/libagora_screen_capture_extension.so"
		)
	}
}

dependencies {
	// Exclude deprecated kotlin-android-extensions-runtime
	// kotlin-parcelize-runtime is automatically included by the kotlin-parcelize plugin
	configurations.all {
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
	}

	implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

	// ───────────────── AndroidX Core ─────────────────
	implementation(libs.androidx.activity)
	implementation(libs.androidx.appcompat)
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.annotation)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.androidx.legacy.support.v4)
	implementation(libs.androidx.browser)
	implementation(libs.androidx.core.splashscreen)

	// ───────────── Lifecycle & Navigation ─────────────
	implementation(libs.androidx.lifecycle.viewmodel.ktx)
	implementation(libs.androidx.lifecycle.livedata.ktx)
	implementation(libs.androidx.navigation.fragment.ktx)
	implementation(libs.androidx.navigation.ui.ktx)

	// ───────────────── Billing ─────────────────
	implementation(libs.billing.ktx)

	// ───────────────── Firebase (BOM) ─────────────────
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.messaging.ktx)
	implementation(libs.firebase.database)
	implementation(libs.firebase.crashlytics)

	// ───────────────── Google / UI ─────────────────
	implementation(libs.material) // DO NOT CHANGE
	implementation(libs.flexbox)

	// ───────────────── Dependency Injection ─────────────────
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)

	// ───────────────── Kotlin / Coroutines ─────────────────
	implementation(libs.kotlinx.coroutines.core)
	// Add kotlinx-metadata-jvm for Kotlin 2.3.0 support with Hilt
	implementation(libs.kotlinx.metadata.jvm)

	// ───────────────── Networking ─────────────────
	implementation(libs.retrofit)
	implementation(libs.converter.gson)
	implementation(libs.logging.interceptor)

	// ───────────────── Image Loading ─────────────────
	implementation(libs.glide)
	implementation(libs.glide.transformations)
	ksp(libs.glide.compiler)

	// ───────────────── Payments ─────────────────
	implementation(libs.stripe.android)

	// ───────────────── Media3 / ExoPlayer ─────────────────
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.exoplayer.dash)
	implementation(libs.androidx.media3.exoplayer.hls)
	implementation(libs.androidx.media3.ui)

	// ───────────────── UI / Widgets / Utilities ─────────────────
	implementation(libs.recyclerview.animators)
	implementation(libs.swipelayout)
	implementation(libs.material.calendar.view)
	implementation(libs.android.image.cropper)
	implementation(libs.android.spinkit)
	implementation(libs.permissionx)
	implementation(libs.toasty)
	implementation(libs.decorator)
	implementation(libs.advanced.card.view)
	implementation(libs.roundedimageview)
	implementation(libs.cameraview)
	implementation(libs.easyvalidation.core)
	implementation(libs.locale.helper.android)
	implementation(libs.html.textview)
	implementation(libs.expandable.layout)
	implementation(libs.slidetoact)
	implementation(libs.immersionbar)
	implementation(libs.immersionbar.ktx)
	implementation(libs.powermenu)
	implementation(libs.singledateandtimepicker)
	implementation(libs.mpandroidchart)
	implementation(libs.number.keyboard)
	implementation(libs.arindicatorview)
	implementation(libs.luckywheelview)
	implementation(libs.aztec)
		implementation("nl.dionsegijn:konfetti-xml:2.0.5")


	// ───────────────── SDKs ─────────────────
	implementation(libs.agora.full.sdk)

	// ───────────────── Socket.IO ─────────────────
	implementation(libs.socket.io.client) {
		exclude(group = "org.json", module = "json")
	}
}
