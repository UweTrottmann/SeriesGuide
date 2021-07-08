package com.battlelancer.seriesguide.backend

import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.HttpExecuteInterceptor
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.firebase.auth.FirebaseUser
import java.io.IOException
import java.util.concurrent.ExecutionException

class FirebaseHttpRequestInitializer : HttpRequestInitializer {

    var firebaseUser: FirebaseUser? = null

    override fun initialize(request: HttpRequest?) {
        request?.interceptor = FirebaseHttpExecuteInterceptor(this)
    }

    @Throws(
        ExecutionException::class, // Tasks.await wraps task exceptions
        InterruptedException::class // Tasks.await
    )
    fun getJwtToken(): String? {
        val firebaseUser = firebaseUser ?: return null

        // https://firebase.google.com/docs/auth/admin/verify-id-tokens
        val task = firebaseUser.getIdToken(true)

        return Tasks.await(task).token
    }
}

private class FirebaseHttpExecuteInterceptor(
    private val firebaseHttpRequestInitializer: FirebaseHttpRequestInitializer
) : HttpExecuteInterceptor {

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

}

class FirebaseAuthIOException(cause: Throwable) : IOException(cause)
