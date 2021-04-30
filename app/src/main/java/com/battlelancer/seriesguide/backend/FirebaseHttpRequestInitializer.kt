package com.battlelancer.seriesguide.backend

import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.HttpExecuteInterceptor
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import java.io.IOException

class FirebaseHttpRequestInitializer : HttpRequestInitializer {

    var firebaseUser: FirebaseUser? = null

    override fun initialize(request: HttpRequest?) {
        request?.interceptor = FirebaseHttpExecuteInterceptor(this)
    }

    @Throws(FirebaseAuthInvalidUserException::class)
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
        try {
            val token = firebaseHttpRequestInitializer.getJwtToken()
            request?.headers?.authorization = "Bearer $token"
        } catch (e: FirebaseAuthException) {
            // Wrap in IOException so it is caught.
            throw FirebaseAuthIOException(e)
        }
    }

}

class FirebaseAuthIOException(cause: FirebaseAuthException) : IOException(cause)
