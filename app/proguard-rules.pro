# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class **.zego.**{*;}

# ============================================================================
# ✅ PRODUCTION FIX: Comprehensive ProGuard Rules for BidSwipe
# ============================================================================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# Retrofit & OkHttp
# ============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Exceptions

# Retrofit interfaces - keep all methods
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp Platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keeppackagenames okhttp3.internal.publicsuffix.*
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# ============================================================================
# Gson - Keep all data models
# ============================================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ✅ CRITICAL: Keep all API response models
-keep class io.bidswipe.app.network.response.** { *; }
-keep class io.bidswipe.app.model.** { *; }
-keep class io.bidswipe.app.base.BaseResponse { *; }

# Keep @SerializedName annotations
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ============================================================================
# Kotlin Coroutines
# ============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================================
# Kotlin Serialization (if used)
# ============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================================================
# AndroidX & Material Components
# ============================================================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**

-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ============================================================================
# ViewBinding
# ============================================================================
-keep class * implements androidx.viewbinding.ViewBinding {
    public static ** bind(android.view.View);
    public static ** inflate(android.view.LayoutInflater);
}

# ============================================================================
# Hilt/Dagger
# ============================================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn com.google.errorprone.annotations.**

# ============================================================================
# Firebase
# ============================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ============================================================================
# Glide
# ============================================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ============================================================================
# Parcelize
# ============================================================================
-keep interface org.jetbrains.annotations.** { *; }
-dontwarn org.jetbrains.annotations.**
-keep class kotlin.Metadata { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============================================================================
# Stripe SDK
# ============================================================================
-keep class com.stripe.** { *; }
-dontwarn com.stripe.**

# ============================================================================
# Socket.IO
# ============================================================================
-keep class io.socket.** { *; }
-keep class com.github.nkzawa.** { *; }
-dontwarn io.socket.**
-dontwarn com.github.nkzawa.**

# ============================================================================
# ExoPlayer
# ============================================================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ============================================================================
# Millicast SDK
# ============================================================================
-keep class com.millicast.** { *; }
-dontwarn com.millicast.**

# ============================================================================
# Custom App Classes - Keep important base classes
# ============================================================================
-keep class io.bidswipe.app.base.** { *; }
-keep class io.bidswipe.app.network.ApiInterface { *; }
-keep class io.bidswipe.app.network.Resource { *; }
-keep class io.bidswipe.app.network.RetrofitService { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Keep Fragment constructors
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

# Keep Activity classes
-keep class * extends androidx.appcompat.app.AppCompatActivity {
    <init>();
}

# ============================================================================
# Enum Classes
# ============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# R8 Full Mode
# ============================================================================
-allowaccessmodification
-repackageclasses