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

# ButterKnife 7
-dontwarn butterknife.internal.**

-keep class butterknife.** { *; }
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

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

# OkHttp 3
-dontwarn okhttp3.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Okio
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Oltu has some stuff not available on Android (javax.servlet), we don't use (slf4j)
# and not included because it is available on Android (json).
-dontwarn javax.servlet.http.**
-dontwarn org.slf4j.**
-dontwarn org.json.**

# Picasso
# Using with OkHttp 3 downloader, but has references to OkHttp2
-dontwarn com.squareup.picasso.OkHttpDownloader

# Retrofit 2.X
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
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

# Assertj
-dontwarn org.assertj.core.**
-dontwarn org.junit.**
-dontwarn java.beans.**

# joda time
-dontwarn org.joda.time.**