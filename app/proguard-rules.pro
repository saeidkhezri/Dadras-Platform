# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*,*TypeAnnotations*

# Preserve Retrofit interfaces and classes
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# Preserve OkHttp3 classes and configurations
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn okio.**

# Preserve Moshi classes and annotations
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepattributes *Annotation*,Signature
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
# Keep generated Moshi adapters
-keep class *JsonAdapter { *; }
-keep class **JsonAdapter { *; }
-keep class com.squareup.moshi.JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep our Application DTOs & Services in com.example.network
-keep class com.example.network.** { *; }
-keepclassmembers class com.example.network.** { *; }

# Keep App Database Entities & DAOs in com.example.data (Room DB persistence)
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-dontwarn com.example.data.**

# Keep our ViewModels and State Management
-keep class com.example.viewmodel.** { *; }
-keepclassmembers class com.example.viewmodel.** { *; }

# Keep standard JSON libraries
-keep class org.json.** { *; }

# Keep Android Platform files
-keep class android.net.** { *; }
-keep class android.os.** { *; }

