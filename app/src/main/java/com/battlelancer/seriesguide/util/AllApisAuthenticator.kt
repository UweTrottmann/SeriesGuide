// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.uwetrottmann.trakt5.TraktV2
import dagger.Lazy
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
        Timber.d("Trakt requires auth.")

        if (responseCount(response) >= 2) {
            Timber.d("Trakt auth failed 2 times, give up.")
            return null
        }

        // verify that we have existing credentials
        val credentials = TraktCredentials.get(context)
        if (credentials.hasCredentials()) {
            // refresh the token
            val successful = credentials.refreshAccessToken(trakt.get())

            if (successful) {
                // retry the request
                return response.request.newBuilder()
                    .header(TraktV2.HEADER_AUTHORIZATION, "Bearer " + credentials.accessToken)
                    .build()
            }
        }
        return null
    }

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
