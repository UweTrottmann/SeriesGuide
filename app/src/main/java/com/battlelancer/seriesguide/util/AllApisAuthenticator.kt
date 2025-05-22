// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.uwetrottmann.trakt5.TraktV2
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject

/**
 * An [Authenticator] that can handle auth for all APIs used with the shared [com.battlelancer.seriesguide.modules.HttpClientModule].
 */
class AllApisAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trakt: Lazy<SgTrakt>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val host = response.request.url.host
        return if (TraktV2.API_HOST == host) {
            handleTraktAuth(response)
        } else {
            null
        }
    }

    private fun handleTraktAuth(response: Response): Request? {
        Timber.d("Trakt requires auth")

        if (responseCount(response) >= 2) {
            Timber.d("Trakt auth failed 2 times, give up")
            return null
        }

        /*
        Note: some online resources suggest to refresh the access token up to half the time before
        it is expired. Probably to avoid a request failing due to an expired access token. However,
        the standard only describes refresh tokens (https://datatracker.ietf.org/doc/html/rfc6749#section-1.5)
        to be used once access tokens are expired or invalid.
        So the approach used here is fine and it's reasonable to expect server implementations to
        handle it.
        */

        val credentials = TraktCredentials.get(context)
        if (credentials.hasCredentials()) {
            // Check if access token used in request is the current one (the request might have been
            // built before refreshing the token due to another failed request has finished).
            val existingAuthHeader = response.request.header(TraktV2.HEADER_AUTHORIZATION)
            val expectedAuthHeader = credentials.getAuthHeader()
            if (existingAuthHeader != expectedAuthHeader) {
                // First try using the current token, it might have been refreshed already
                Timber.d("Trakt access token outdated, trying again with current one")
                return response.request.buildNewWithTraktAuthHeader(expectedAuthHeader)
            }

            // Refresh the access token or wait on a running refresh
            val successful = try {
                runBlocking {
                    credentials.refreshAccessTokenAsync(trakt.get())
                }
            } catch (e: InterruptedException) {
                false // This thread may get interrupted, causing the coroutine to throw
            }

            if (successful) {
                // retry the request
                return response.request.buildNewWithTraktAuthHeader(credentials.getAuthHeader())
            }
        }

        return null
    }

    private fun TraktCredentials.getAuthHeader(): String = "Bearer $accessToken"

    private fun Request.buildNewWithTraktAuthHeader(authHeader: String): Request = newBuilder()
        .header(TraktV2.HEADER_AUTHORIZATION, authHeader)
        .build()

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
