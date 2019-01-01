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


## Retrofit 2 (https://github.com/square/retrofit/blob/0b3bc2d8e155f2a7edec46d247fd9973cf3c46f9/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.-KotlinExtensions


## Okio (via Retrofit 2) (https://github.com/square/okio/blob/b080ca7bf9436dd7fcc7a593c8845c0919ab80d2/okio/jvm/src/main/resources/META-INF/proguard/okio.pro)
-dontwarn org.codehaus.mojo.animal_sniffer.*


## OkHttp3 (via Retrofit 2) (https://github.com/square/okhttp/blob/a16ec15ee08424058b26c6bd62afc98b32df98bc/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro)
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
