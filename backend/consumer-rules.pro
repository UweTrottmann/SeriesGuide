# Cloud Endpoints libraries
# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection.
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
