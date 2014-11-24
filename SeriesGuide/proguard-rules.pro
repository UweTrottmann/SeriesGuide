# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
# Partially copied from flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Only obfuscate to avoid potential clashes with exported libraries
-dontshrink
-dontoptimize
-dontpreverify

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

# App Engine libs use annotations not available on Android.
-dontwarn sun.misc.Unsafe

# Google Play Services is stripped of unused parts. Don't warn about them missing.
-dontwarn com.google.ads.**
-dontwarn com.google.android.gms.**

# ButterKnife uses some javax.annotation classes Android does not ship with.
-dontwarn butterknife.internal.**

# Eventbus methods can not be renamed.
-keepclassmembers class ** {
    public void onEvent*(**);
}

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