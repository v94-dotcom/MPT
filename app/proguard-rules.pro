# MPT ProGuard/R8 Rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable data classes
-keep,includedescriptorclasses class com.mpt.masterpasswordtrainer.data.model.**$$serializer { *; }
-keepclassmembers class com.mpt.masterpasswordtrainer.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.mpt.masterpasswordtrainer.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Argon2kt native library ---
-keep class com.lambdapioneer.argon2kt.** { *; }
-keepclassmembers class com.lambdapioneer.argon2kt.** { *; }

# --- AndroidX Security / EncryptedSharedPreferences ---
-keep class androidx.security.crypto.** { *; }

# --- Google Tink (used by EncryptedSharedPreferences) ---
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# --- Android Keystore ---
-keep class android.security.keystore.** { *; }
