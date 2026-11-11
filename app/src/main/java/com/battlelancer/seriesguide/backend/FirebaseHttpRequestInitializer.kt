// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

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
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Adds authorization header using Firebase JWT token to each request for current Firebase user.
 * Uses the token cached by Firebase Auth, which refreshes it only if it is expired or about to.
 * If a request fails with HTTP 401 tries once with a force refreshed token.
 *
 * Note that if the thread that executes a request using this is interrupted, the token request
 * fails with a [FirebaseAuthIOException] (the request client expects this to only throw
 * [IOException], so this wraps [InterruptedException]). This clears the interrupted flag as
 * [Tasks.await] does.
 */
class FirebaseHttpRequestInitializer : HttpRequestInitializer {

    var firebaseUser: FirebaseUser? = null

    override fun initialize(request: HttpRequest?) {
        val requestHandler = FirebaseHttpExecuteInterceptor(this)
        request?.interceptor = requestHandler
        request?.unsuccessfulResponseHandler = requestHandler
    }

    /**
     * Returns the ID token of the current user, or null if there is none.
     *
     * If [forceRefresh] is false, returns the token cached by Firebase Auth if it is still valid
     * (it expires after 1 hour). This avoids network requests and FirebaseTooManyRequestsException.
     * Set [forceRefresh] to true if a request failed with HTTP 401 Unauthorized, for example
     * because the account has been disabled, deleted, or its credentials are no longer valid.
     */
    @Throws(
        ExecutionException::class, // Tasks.await wraps task exceptions
        InterruptedException::class, // Tasks.await interrupted
        TimeoutException::class // Tasks.await timed out
    )
    fun getJwtToken(forceRefresh: Boolean): String? {
        val firebaseUser = firebaseUser ?: return null

        // https://firebase.google.com/docs/auth/admin/verify-id-tokens
        // If not forced, returns the cached token without a network request, unless it expires
        // within the next 5 minutes (checked for firebase-auth 24.0.1, also documented for
        // JavaScript API https://firebase.google.com/docs/reference/js/auth.user#usergetidtoken).
        val task = firebaseUser.getIdToken(forceRefresh)

        // https://developers.google.com/android/guides/tasks
        return Tasks.await(task, 20, TimeUnit.SECONDS).token
    }
}

private class FirebaseHttpExecuteInterceptor(
    private val firebaseHttpRequestInitializer: FirebaseHttpRequestInitializer
) : HttpExecuteInterceptor, HttpUnsuccessfulResponseHandler {

    private var received401: Boolean = false

    override fun intercept(request: HttpRequest?) {
        // Note: wrap exceptions to not crash calling code that expects IOException.
        try {
            val token = firebaseHttpRequestInitializer.getJwtToken(forceRefresh = received401)
            request?.headers?.authorization = "Bearer $token"
        } catch (e: ExecutionException) {
            throw FirebaseAuthIOException(e.cause ?: e)
        } catch (e: Exception) {
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
            // Signal to retry request, intercept will then force refresh the token.
            true
        } else {
            // Not 401 or already tried to force refresh the token once for this request.
            false
        }
    }

}

class FirebaseAuthIOException(cause: Throwable) : IOException(cause)
