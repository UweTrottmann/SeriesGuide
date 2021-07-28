package com.battlelancer.seriesguide.backend

import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.HttpExecuteInterceptor
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.HttpUnsuccessfulResponseHandler
import com.google.firebase.auth.FirebaseUser
import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * Adds authorization header using Firebase JWT token to each request for current Firebase user.
 * Fetches token once and caches it between requests.
 * If a request fails with HTTP 401 tries once to fetch token again.
 */
class FirebaseHttpRequestInitializer : HttpRequestInitializer {

    var firebaseUser: FirebaseUser? = null
    private var token: String? = null

    override fun initialize(request: HttpRequest?) {
        val requestHandler = FirebaseHttpExecuteInterceptor(this)
        request?.interceptor = requestHandler
        request?.unsuccessfulResponseHandler = requestHandler
    }

    @Synchronized
    @Throws(
        ExecutionException::class, // Tasks.await wraps task exceptions
        InterruptedException::class // Tasks.await
    )
    fun getJwtToken(): String? {
        val firebaseUser = firebaseUser ?: return null

        // Return cached token to avoid FirebaseTooManyRequestsException
        // at the risk of requests failing with HTTP 401 Unauthorized if
        // the account has been disabled, deleted, or its credentials are no longer valid.
        token?.let { return it }

        // https://firebase.google.com/docs/auth/admin/verify-id-tokens
        val task = firebaseUser.getIdToken(true)

        return Tasks.await(task).token.also {
            token = it
        }
    }

    @Synchronized
    fun clearJwtToken() {
        token = null
    }
}

private class FirebaseHttpExecuteInterceptor(
    private val firebaseHttpRequestInitializer: FirebaseHttpRequestInitializer
) : HttpExecuteInterceptor, HttpUnsuccessfulResponseHandler {

    private var received401: Boolean = false

    override fun intercept(request: HttpRequest?) {
        // Note: wrap exceptions to not crash calling code that expects IOException.
        try {
            val token = firebaseHttpRequestInitializer.getJwtToken()
            request?.headers?.authorization = "Bearer $token"
        } catch (e: ExecutionException) {
            throw FirebaseAuthIOException(e.cause ?: e)
        } catch (e: InterruptedException) {
            throw FirebaseAuthIOException(e)
        }
    }

    override fun handleResponse(
        request: HttpRequest?,
        response: HttpResponse?,
        supportsRetry: Boolean
    ): Boolean {
        return if (response?.statusCode == 401 && !received401) {
            received401 = true
            // Clear token and signal to retry request so token is fetched again.
            firebaseHttpRequestInitializer.clearJwtToken()
            true
        } else {
            // Not 401 or already tried to clear and fetch token once for this request.
            false
        }
    }

}

class FirebaseAuthIOException(cause: Throwable) : IOException(cause)
