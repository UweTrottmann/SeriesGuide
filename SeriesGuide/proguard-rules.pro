# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Only obfuscate to avoid potential clashes with globally exported libraries we use as well
-dontshrink

# SeriesGuide, keep everything
-keep class com.battlelancer.seriesguide.** { *; }

# getglue-java
-keep class com.uwetrottmann.getglue.** { *; }
# tmdb-java
-keep class com.uwetrottmann.tmdb.** { *; }
# trakt-java
-keep class com.jakewharton.trakt.** { *; }

# App Engine libs use annotations not available on Android.
-dontwarn sun.misc.Unsafe
# Needed to keep generic types and @Key annotations accessed via reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# Google Play Services is stripped of unused parts. Don't warn about them missing.
-dontwarn com.google.ads.**
-dontwarn com.google.android.gms.**

# ButterKnife uses some annotations not available on Android.
-dontwarn butterknife.internal.**
# Prevent ButterKnife annotations from getting renamed.
-keep class **$$ViewInjector { *; }
-keepnames class * { @butterknife.InjectView *;}

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
-keep class retrofit.** { *; }