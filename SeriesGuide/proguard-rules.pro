# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Only obfuscate
-dontshrink

# Keep everything, but obfuscate the v7 support library
# to work around wrongly exported libraries on some 4.2.2 ROMs
# see https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.** { *; }
-repackageclasses 'com.uwetrottmann.obfuscated'
-allowaccessmodification

# Google Play Services is stripped of unused parts. Don't warn about them missing.
-dontwarn com.google.ads.**
-dontwarn com.google.android.gms.**

# ButterKnife uses some annotations not available on Android.
-dontwarn butterknife.internal.**
# Prevent ButterKnife annotations from getting renamed.
-keepnames class * { @butterknife.InjectView *;}

# Eventbus methods can not be renamed.
-keepclassmembers class ** {
    public void onEvent*(**);
}

# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature
# Gson specific classes
-dontwarn sun.misc.Unsafe

# joda-time has some annotations we don't care about.
-dontwarn org.joda.convert.**

# OkHttp has some internal stuff not available on Android.
-dontwarn com.squareup.okhttp.internal.**

# Okio has some stuff not available on Android.
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Oltu has some stuff not available on Android (javax.servlet), we don't use (slf4j)
# and not included because it is available on Android (json).
-dontwarn javax.servlet.http.**
-dontwarn org.slf4j.**
-dontwarn org.json.**

# Retrofit has some optional dependencies we don't use.
-dontwarn rx.**
-dontwarn retrofit.appengine.**
