# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in [sdk-path]/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile


## Apache Commons Text
-dontwarn javax.script.**


## Gson (https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg)
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class nl.jpelgrm.movienotifier.models.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer


## Retrofit 2 (http://square.github.io/retrofit/#download)
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Exceptions


## Okio (via Retrofit 2) (https://github.com/square/okio#proguard)
-dontwarn okio.**


## OkHttp3 (via Retrofit 2) (https://github.com/square/okhttp#proguard)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
