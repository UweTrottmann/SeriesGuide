# Testing and debugging

## Retrofit

To quickly test error responses, replace `call.execute()` with something like:

```kotlin
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

Response.error<T>(404, "Item not found".toResponseBody("text/plain; charset=utf-8".toMediaType()))
```
