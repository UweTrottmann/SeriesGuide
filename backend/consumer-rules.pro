# Cloud Endpoints libraries

# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection.
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# R8 full mode: model classes are only instantiated via reflection by google-http-client,
# keep the default constructor so they are not treated as never instantiated.
# GenericData covers both request and model classes.
-keepclassmembers class * extends com.google.api.client.util.GenericData {
  public <init>();
}
