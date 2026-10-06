# Proguard rules for Quacky

# Keep ML Kit Barcode
-keep class com.google.mlkit.** { *; }

# Keep ZXing
-keep class com.google.zxing.** { *; }

# Keep ARCore
-keep class com.google.ar.core.** { *; }

# Keep Room
-keep class androidx.room.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
