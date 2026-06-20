# DocMaster AI ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep iText
-keep class com.itextpdf.** { *; }

# Keep Billing
-keep class com.android.billingclient.** { *; }

# Keep data classes
-keep class com.ndroid.docmasterai.data.model.** { *; }
-keep class com.ndroid.docmasterai.data.local.entity.** { *; }

# Keep serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
