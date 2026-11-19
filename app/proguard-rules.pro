# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Don't obfuscate anything, it's slow and makes issues harder to diagnose
-dontobfuscate
# Output code detected as unused (and stripped) for analysis
-printusage unused.txt

# Amazon Appstore SDK (3.0.5 release no longer includes ProGuard rules)
# https://developer.amazon.com/docs/in-app-purchasing/iap-obfuscate-the-code.html
# Note: the "Expected stack map table for method with non-linear control flow." warning is caused by
# classes in the amazon-appstore-sdk JAR not containing StackMapTable attributes.
-dontwarn com.amazon.**
-keep class com.amazon.** {*;}
-keepattributes *Annotation*

# R8 full mode
# Retrofit model classes created via reflection
-keep class com.battlelancer.seriesguide.tmdbapi.WatchProviderResults { *; }

# R8 obfuscation (currently disabled, but collecting rules in any case)
# Prevent renaming of androidx package: EventBus otherwise doesn't stop walking
# the class hierarchy at "androidx." classes (see SubscriberMethodFinder). This
# prevents NoClassDefFoundError on older Android versions as it hits signatures
# using new APIs (such as PictureInPictureUiState < API 31).
#-keepnames class androidx.** extends android.app.Activity
#-keepnames class androidx.** extends androidx.fragment.app.Fragment
