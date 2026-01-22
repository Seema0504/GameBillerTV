# Add project specific ProGuard rules here.

# General
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions

# Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okhttp3.logging.** { *; }
-dontwarn okhttp3.logging.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# DataStore
-keep class androidx.datastore.*.** { *; }

# Timber
-dontwarn org.jetbrains.annotations.**

# Project Models & Data (Critical)
-keep class com.gamebiller.tvlock.data.** { *; }
-keepclassmembers class com.gamebiller.tvlock.data.** { *; }
-keep class com.gamebiller.tvlock.domain.** { *; }
-keepclassmembers class com.gamebiller.tvlock.domain.** { *; }
-keepclassmembers enum * { *; }

# Keep Lifecycle observers
-keep class androidx.lifecycle.** { *; }
-keep class androidx.startup.** { *; }

