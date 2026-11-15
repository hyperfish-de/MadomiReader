# Keep Gson model classes used by the Gemini REST client.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.madomireader.data.remote.gemini.** { *; }
-keep class com.example.madomireader.domain.model.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
