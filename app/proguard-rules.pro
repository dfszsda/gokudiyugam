# Firebase and Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep all models in the model package to prevent R8/ProGuard from obfuscating them.
# This is crucial for Firestore and Gson deserialization.
-keep class com.example.gokudiyugam.model.** { *; }
-keepclassmembers class com.example.gokudiyugam.model.** {
    <fields>;
    <init>();
}

# Keep the UserRole enum
-keepclassmembers enum com.example.gokudiyugam.model.UserRole {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Handle Kotlin Serialization/Coroutines in Release
-keepattributes Signature, *Annotation*, InnerClasses
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# Fix R8 issues with Micrometer and Reactor (often brought in by some libraries)
-dontwarn io.micrometer.context.**
-dontwarn reactor.blockhound.**
-dontwarn reactor.util.context.ReactorContextAccessor

# Keep Compose related classes if needed
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }

# Google Drive API
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
