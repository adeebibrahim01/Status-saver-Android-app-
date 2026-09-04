# --- Existing Rules ---
-keep class com.bumptech.glide.** { *; }
-keep class com.yalantis.ucrop** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.mariaxcodexpert.whatsdownloadplus.ui.ImagesAndVideo.** { *; }

# --- Firebase & Data Parsing Fix ---
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keep class com.mariaxcodexpert.whatsdownloadplus.ui.trending.TrendMediaItem { *; }
-keepclassmembers class com.mariaxcodexpert.whatsdownloadplus.ui.trending.TrendMediaItem { *; }
-keep class com.google.firebase.database.** { *; }
-keep class com.mariaxcodexpert.whatsdownloadplus.data.** { *; }

# --- Ads & Analytics (Meta & Unity) ---
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**

# --- Support System Models ---
-keep class com.mariaxcodexpert.whatsdownloadplus.ui.support.** { *; }
-keepclassmembers class com.mariaxcodexpert.whatsdownloadplus.ui.support.** { *; }

# =========================================================
# NEW UPDATES FOR STABILITY & MEDIA3
# =========================================================

# Media3 & UI components fix (Warning handle karne ke liye)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# R8/Jetifier Compatibility rules
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**
-dontwarn android.support.**

# General stability for common libraries
-keepattributes EnclosingMethod,InnerClasses
-dontwarn okio.**
-dontwarn javax.annotation.**

# =========================================================
# FINAL ADDITIONS FOR 100M+ SCALE STABILITY
# =========================================================

# 1. Gson (Data serialization ke liye zaruri hai, warna crash hota hai)
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# 2. Retrofit/OkHttp (Networking stability ke liye)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# 3. WorkManager (Background tasks ke liye)
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# --- FIX FOR SECURITY EXCEPTION & GMS ---
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.common.** { *; }

# Important for NotificationListenerService stability
-keep class com.mariaxcodexpert.whatsdownloadplus.services.** { *; }
-keepclassmembers class com.mariaxcodexpert.whatsdownloadplus.services.** { *; }

# Keep Lifecycle to avoid issues during app lifecycle events
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**