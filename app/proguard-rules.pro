# Add project specific ProGuard rules here.
# Keep Moshi DTO classes and generated adapters for release APKs
-keepclassmembers class com.example.data.remote.* { *; }
-keep class com.example.data.remote.*JsonAdapter { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
