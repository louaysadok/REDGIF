# kotlinx.serialization: keep generated serializers for DTOs and models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.louaysadok.redgif.**$$serializer { *; }
-keepclassmembers class com.louaysadok.redgif.** { *** Companion; }
-keepclasseswithmembers class com.louaysadok.redgif.** { kotlinx.serialization.KSerializer serializer(...); }
