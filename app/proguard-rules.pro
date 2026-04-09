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

# --- Keep all data model classes ---
-keep class com.mpt.masterpasswordtrainer.data.model.** { *; }

# --- Argon2kt native library ---
-keep class com.lambdapioneer.argon2kt.** { *; }
-keepclassmembers class com.lambdapioneer.argon2kt.** { *; }

# --- AndroidX Security / EncryptedSharedPreferences ---
-keep class androidx.security.crypto.** { *; }

# --- Google Tink (used internally by EncryptedSharedPreferences) ---
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# --- Android Keystore ---
-keep class android.security.keystore.** { *; }

# --- Kotlin Metadata ---
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --- ViewModels (reflective instantiation by Compose viewModel()) ---
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# --- WorkManager (Room database + workers use reflection) ---
-keep class androidx.work.** { *; }

# --- BroadcastReceivers ---
-keep class com.mpt.masterpasswordtrainer.worker.BootReceiver { *; }

# --- AppWidgetProviders ---
-keep class com.mpt.masterpasswordtrainer.widget.** { *; }

# --- AndroidX Biometric ---
-keep class androidx.biometric.** { *; }

# --- General safety ---
-keepattributes Signature
-keepattributes Exceptions
