# Firebase and Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep models for Firestore/RTDB
-keep @androidx.annotation.Keep class com.example.gokudiyugam.model.** { *; }
-keepclassmembers class com.example.gokudiyugam.model.** {
    <fields>;
    <init>();
}

# Handle Kotlin Serialization/Coroutines in Release
-keepattributes Signature, *Annotation*, InnerClasses
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# MediaItem specific
-keep class com.example.gokudiyugam.model.MediaItem { *; }
