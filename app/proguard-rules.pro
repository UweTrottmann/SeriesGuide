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

# Do not shrink any of this apps code (unused code should be deleted instead)
-keep class com.battlelancer.** { *; }

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
# warnings due to removed old Guava version used in test classes
-dontwarn com.google.api.client.googleapis.testing.**

# OkHttp 3
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Okio
-dontwarn okio.**

# Picasso
# Using with OkHttp 3 downloader, but has references to OkHttp2
-dontwarn com.squareup.picasso.OkHttpDownloader

# Retrofit 2.X
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# Apache HTTP was removed as of Android M
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.api.client.http.apache.**
-dontwarn com.google.android.gms.internal.**

## Testing
-dontwarn android.test.**

# Ignore some notes about unused classes referenced in method signatures
-dontnote com.tonicartos.widget.stickygridheaders.**
-dontnote com.uwetrottmann.thetvdb.**
-dontnote com.uwetrottmann.tmdb2.**
-dontnote com.uwetrottmann.trakt5.**
-dontnote uk.co.senab.photoview.**
