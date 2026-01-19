# Add project specific ProGuard rules here.

# Keep Retrofit and Moshi classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class com.gamebiller.tvlock.data.remote.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep DataStore
-keep class androidx.datastore.*.** { *; }

# Timber
-dontwarn org.jetbrains.annotations.**
