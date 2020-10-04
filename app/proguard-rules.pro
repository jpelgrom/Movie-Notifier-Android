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


## Okio (via Retrofit 2) (https://github.com/square/okio/blob/da3112eda6aff08be5d3f937c8a6422cf07ea697/okio/src/jvmMain/resources/META-INF/proguard/okio.pro)
-dontwarn org.codehaus.mojo.animal_sniffer.*