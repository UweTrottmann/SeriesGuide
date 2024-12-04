// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response

/**
 * Extends [TraktV2] to use custom caching OkHttp client and [TraktCredentials] to store user
 * credentials.
 */
class SgTrakt(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) : TraktV2(
    BuildConfig.TRAKT_CLIENT_ID,
    BuildConfig.TRAKT_CLIENT_SECRET,
    BaseOAuthActivity.OAUTH_CALLBACK_URL_CUSTOM
) {
    override fun accessToken(): String? {
        return TraktCredentials.get(context).accessToken
    }

    override fun refreshToken(): String? {
        return TraktOAuthSettings.getRefreshToken(context)
    }

    @Synchronized
    override fun okHttpClient(): OkHttpClient {
        return okHttpClient
    }

    fun sgComments(): TraktComments {
        return TraktComments(context, this)
    }

    companion object {

        /**
         * Returns if the request was not authorized. If it was, also calls
         * [TraktCredentials.setCredentialsInvalid] to notify the user.
         */
        @JvmStatic
        fun isUnauthorized(context: Context, response: Response<*>): Boolean {
            if (isUnauthorized(response)) {
                // current access token is invalid, remove it and notify user to re-connect
                TraktCredentials.get(context).setCredentialsInvalid()
                return true
            } else {
                return false
            }
        }

        fun checkForTraktError(trakt: TraktV2, response: Response<*>): String? {
            val error = trakt.checkForTraktError(response)
            return if (error?.message != null) {
                error.message
            } else {
                null
            }
        }

        fun checkForTraktOAuthError(trakt: TraktV2, response: Response<*>): String? {
            val error = trakt.checkForTraktOAuthError(response)
            return if (error?.error != null && error.error_description != null) {
                error.error + " " + error.error_description
            } else {
                null
            }
        }

        /**
         * Executes the given call. Will return null if the call fails for any reason, including auth
         * failures.
         */
        fun <T> executeCall(call: Call<T>, action: String): T? {
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    return response.body()
                } else {
                    Errors.logAndReport(action, response)
                }
            } catch (e: Exception) {
                Errors.logAndReport(action, e)
            }
            return null
        }

        /**
         * Executes the given call. If the call fails because auth is invalid, removes the current
         * access token and displays a warning notification to the user.
         */
        fun <T> executeAuthenticatedCall(context: Context, call: Call<T>, action: String): T? {
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    return response.body()
                } else {
                    if (!isUnauthorized(context, response)) {
                        Errors.logAndReport(action, response)
                    }
                }
            } catch (e: Exception) {
                Errors.logAndReport(action, e)
            }
            return null
        }
    }
}
