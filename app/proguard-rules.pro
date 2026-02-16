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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========== GSON ==========
# Keep all data classes used with Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Keep generic type information for Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep all ESDE widget and preference data classes
-keep class jr.brian.home.esde.widget.OverlayWidget { *; }
-keep class jr.brian.home.esde.widget.OverlayWidget$** { *; }
-keep class jr.brian.home.esde.preferences.** { *; }
-keep class jr.brian.home.esde.wallpaper.WallpaperState { *; }

# Keep all enum classes (ImageType, WidgetContext, ScaleType, etc.)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== COIL ==========
# Coil image loading library
-keep class coil.** { *; }
-keep interface coil.** { *; }
-keep class coil.decode.** { *; }
-dontwarn coil.**

# ========== EXOPLAYER ==========
# Keep ExoPlayer classes for video playback
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ========== HILT ==========
# Hilt already has its own rules, but ensure generated classes aren't stripped
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn jr.brian.home.JarngreiprApplication_GeneratedInjector

# ========== KOTLIN COROUTINES ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ========== FILE I/O ==========
# Keep FileObserver for ES-DE event monitoring
-keep class jr.brian.home.esde.events.** { *; }
-keep class android.os.FileObserver { *; }

# ========== COMPOSE ==========
# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-dontwarn androidx.compose.**