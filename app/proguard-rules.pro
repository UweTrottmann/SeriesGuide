# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Currently no need to obfuscate anything
-dontobfuscate
# Output unused code so we may optimize it
-printusage unused.txt

# Ignore notes about reflection use in support library
-dontnote android.support.**

# Cloud Endpoints libraries
# Needed to keep generic types and @Key annotations accessed via reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# Amazon IAP library
-dontwarn com.amazon.**
-keep class com.amazon.** { *; }
#-keepattributes *Annotation* // already in default config

# Crashlytics 2.+
-keep class com.crashlytics.** { *; }
-keep class com.crashlytics.android.**
-keepattributes SourceFile, LineNumberTable
#-keepattributes *Annotation* // already in default config

# EventBus
# Keep subscriber methods
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature
# Gson specific classes
-dontwarn sun.misc.Unsafe

# Google API client
# warnings due to Guava classes used in tests and ErrorProne annotations
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn com.google.api.client.http.apache.** # Apache HTTP was removed as of Android M
-dontwarn com.google.api.client.util.** # Unused Apache commons-codec code

# OkHttp 3
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# Retrofit 2.x
# Keep entity and enum classes. R8 may strip unused, but required fields from entities.
-keep class com.uwetrottmann.trakt5.entities.** { *; }
-keep class com.uwetrottmann.trakt5.enums.** { *; }
-keep class com.uwetrottmann.tmdb2.entities.** { *; }
-keep class com.uwetrottmann.tmdb2.enumerations.** { *; }

# Apache HTTP was removed as of Android M
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.internal.**

## Testing
-dontwarn android.test.**

# Ignore some notes about unused classes referenced in method signatures
-dontnote uk.co.senab.photoview.**
