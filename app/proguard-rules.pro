# Add project specific ProGuard rules here.

# ─── Kotlin & Coroutines ───────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ─── Room Database ────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ─── Hilt / Dagger ────────────────────────────────────────────────────────────
-dontwarn dagger.**
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltWrapper_** { *; }

# ─── Gson ─────────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── AdMob ────────────────────────────────────────────────────────────────────
-keep public class com.google.android.gms.ads.** { public *; }
-dontwarn com.google.android.gms.**

# ─── Google Play Billing ──────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ─── Vico Charts ──────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ─── Jetpack Glance Widget ────────────────────────────────────────────────────
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# ─── App Data Models ──────────────────────────────────────────────────────────
-keep class com.khatibstudio.noyza.data.** { *; }
-keep class com.khatibstudio.noyza.domain.** { *; }

# ─── BuildConfig ──────────────────────────────────────────────────────────────
-keep class com.khatibstudio.noyza.BuildConfig { *; }
