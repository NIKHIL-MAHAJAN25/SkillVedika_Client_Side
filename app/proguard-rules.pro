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
# ---- Retrofit ----
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep,allowobfuscation interface *
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# ---- Gson ----
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
# Firestore POJO serialization — keep all data classes intact
-keepclassmembers class com.nikhil.buyerapp.dataclasses.** {
    *;
}

# Keep Kotlin data class component functions and getters
-keepclassmembers class com.nikhil.buyerapp.** {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Firestore itself
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# ---- Your data/network model classes (critical for Gson/Retrofit) ----
-keep class com.nikhil.buyerapp.dataclasses.** { *; }
-keep class com.nikhil.buyerapp.mailretro.** { *; }

# ---- PDFBox optional JPEG2000 codec (unused, prevents R8 missing-class error) ----
-dontwarn com.gemalto.jp2.**
-keep class com.gemalto.jp2.** { *; }
-keep class com.nikhil.buyerapp.** { *; }