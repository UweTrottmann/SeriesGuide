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
# Output unused code so it can be optimized
-printusage unused.txt

# Amazon Appstore SDK (3.0.5 release no longer includes ProGuard rules)
# https://developer.amazon.com/docs/in-app-purchasing/iap-obfuscate-the-code.html
-dontwarn com.amazon.**
-keep class com.amazon.** {*;}
-keepattributes *Annotation*
