package com.battlelancer.seriesguide.traktapi

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.sync.NetworkJobProcessor
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.AccessToken
import com.uwetrottmann.trakt5.entities.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.awaitResponse
import timber.log.Timber
import java.io.IOException
import com.battlelancer.seriesguide.enums.Result as SgResult

class TraktAuthActivityModel(application: Application) : AndroidViewModel(application) {

    data class ConnectResult(
        /**
         * One of {@link TraktResult}.
         */
        val code: Int,
        val debugMessage: String? = null
    )

    var connectInProgress: Boolean = false
    val connectResult = MutableLiveData<ConnectResult>()

    /**
     * Expects a valid Trakt OAuth auth code. Retrieves the access token and username
     * for the associated user. If successful, the credentials and user info are stored.
     */
    fun connectTrakt(authCode: String?) = viewModelScope.launch(Dispatchers.Default) {
        connectInProgress = true

        @Suppress("CascadeIf", "BlockingMethodInNonBlockingContext")
        val taskResult = withContext(Dispatchers.IO) {
            // check for connectivity
            if (!AndroidUtils.isNetworkConnected(getApplication())) {
                return@withContext ConnectResult(TraktResult.OFFLINE)
            }

            // check if we have any usable data
            if (authCode.isNullOrEmpty()) {
                Timber.e("Failed because auth code is empty.")
                return@withContext ConnectResult(TraktResult.AUTH_ERROR)
            }

            val trakt = SgApp.getServicesComponent(getApplication()).trakt()

            // get access token
            var accessToken: String? = null
            var refreshToken: String? = null
            var expiresIn: Long = -1
            try {
                val response: Response<AccessToken> = trakt.exchangeCodeForAccessToken(authCode)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    accessToken = body.access_token
                    refreshToken = body.refresh_token
                    expiresIn = body.expires_in?.toLong() ?: -1L
                } else {
                    Errors.logAndReport(
                        "get access token", response,
                        SgTrakt.checkForTraktOAuthError(trakt, response)
                    )
                    return@withContext ConnectResult(
                        TraktResult.AUTH_ERROR,
                        "get access token HTTP " + response.code()
                    )
                }
            } catch (e: IOException) {
                Errors.logAndReport("get access token", e)
            }

            // did we obtain all required data?
            if (accessToken.isNullOrEmpty()) {
                Timber.e("Failed to obtain access token.")
                return@withContext ConnectResult(TraktResult.AUTH_ERROR)
            } else if (refreshToken.isNullOrEmpty()) {
                Timber.e("Failed to obtain refresh token")
                return@withContext ConnectResult(TraktResult.AUTH_ERROR)
            } else if (expiresIn < 1) {
                Timber.e("Failed because no valid expiry time.")
                return@withContext ConnectResult(TraktResult.AUTH_ERROR)
            }

            // reset sync state before hasCredentials may return true
            NetworkJobProcessor(getApplication()).removeObsoleteJobs(false)
            PreferenceManager.getDefaultSharedPreferences(getApplication()).edit {
                // make next sync merge local watched and collected episodes with those on trakt
                putBoolean(TraktSettings.KEY_HAS_MERGED_EPISODES, false)
                // make next sync merge local movies with those on trakt
                putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, false)

                // make sure the next sync will run a full episode sync
                putLong(TraktSettings.KEY_LAST_FULL_EPISODE_SYNC, 0)
                // make sure the next sync will download all watched movies
                putLong(TraktSettings.KEY_LAST_MOVIES_WATCHED_AT, 0)
                // make sure the next sync will download all ratings
                putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0)
                putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0)
                putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, 0)
            }

            // store the access token, refresh token and expiry time
            TraktCredentials.get(getApplication()).storeAccessToken(accessToken)
            if (!TraktCredentials.get(getApplication()).hasCredentials()) {
                // saving access token failed, abort.
                Timber.e("Failed because access token can not be stored.")
                return@withContext ConnectResult(SgResult.ERROR, "access token not stored")
            }
            if (!TraktOAuthSettings.storeRefreshData(getApplication(), refreshToken, expiresIn)) {
                // saving refresh token failed, abort.
                Timber.e("Failed because refresh data can not be stored.")
                TraktCredentials.get(getApplication()).removeCredentials()
                return@withContext ConnectResult(SgResult.ERROR, "refresh token not stored")
            }

            // get user and display name
            var username: String? = null
            var displayname: String? = null
            try {
                val response: Response<Settings> = trakt.users().settings().awaitResponse()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val user = body.user
                    if (user != null) {
                        username = user.username
                        displayname = user.name
                    }
                } else {
                    Errors.logAndReport(
                        "get user settings", response,
                        SgTrakt.checkForTraktError(trakt, response)
                    )
                    return@withContext when {
                        SgTrakt.isUnauthorized(response) -> {
                            // access token already is invalid, remove it :(
                            TraktCredentials.get(getApplication()).removeCredentials()
                            ConnectResult(TraktResult.AUTH_ERROR)
                        }
                        SgTrakt.isAccountLocked(response) -> {
                            ConnectResult(TraktResult.ACCOUNT_LOCKED)
                        }
                        else -> {
                            ConnectResult(
                                TraktResult.AUTH_ERROR,
                                "get user settings HTTP " + response.code()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Errors.logAndReport("get user settings", e)
                return@withContext ConnectResult(
                    if (AndroidUtils.isNetworkConnected(getApplication())) {
                        TraktResult.API_ERROR
                    } else {
                        TraktResult.OFFLINE
                    }
                )
            }

            // did we obtain a username (display name is not required)?
            if (username.isNullOrEmpty()) {
                Timber.e("Failed because returned user name is empty.")
                return@withContext ConnectResult(TraktResult.API_ERROR)
            }
            TraktCredentials.get(getApplication()).storeUsername(username, displayname)

            Timber.i("Successfully connected to Trakt.")
            return@withContext ConnectResult(SgResult.SUCCESS)
        }

        connectInProgress = false
        connectResult.postValue(taskResult)
    }

}