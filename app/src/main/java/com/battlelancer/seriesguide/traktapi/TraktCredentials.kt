package com.battlelancer.seriesguide.traktapi

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.sync.AccountUtils
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.PendingIntentCompat
import com.uwetrottmann.trakt5.TraktV2
import timber.log.Timber
import java.io.IOException

/**
 * A singleton helping to manage the user's trakt credentials.
 */
class TraktCredentials private constructor(context: Context) {

    private val context: Context = context.applicationContext
    private var hasCredentials: Boolean

    /**
     * Get the username.
     */
    var username: String?
        private set

    init {
        username = PreferenceManager.getDefaultSharedPreferences(this.context)
            .getString(KEY_USERNAME, null)
        hasCredentials = !TextUtils.isEmpty(accessToken)
    }

    /**
     * If there is a username and access token.
     */
    fun hasCredentials(): Boolean {
        return hasCredentials
    }

    /**
     * Removes the current trakt access token (but not the username), so [.hasCredentials]
     * will return `false`, and shows a notification asking the user to re-connect.
     */
    @Synchronized
    fun setCredentialsInvalid() {
        if (!hasCredentials) {
            // already invalidated credentials
            return
        }

        removeAccessToken()
        Timber.e("trakt credentials invalid, removed access token")

        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS)
        NotificationSettings.setDefaultsForChannelErrors(context, nb)

        nb.setSmallIcon(R.drawable.ic_notification)
        nb.setContentTitle(context.getString(R.string.trakt_reconnect))
        nb.setContentText(context.getString(R.string.trakt_reconnect_details))
        nb.setTicker(context.getString(R.string.trakt_reconnect_details))

        val intent = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, ShowsActivity::class.java))
            .addNextIntent(Intent(context, ConnectTraktActivity::class.java))
            .getPendingIntent(
                0,
                PendingIntentCompat.flagImmutable or PendingIntent.FLAG_UPDATE_CURRENT
            )
        nb.setContentIntent(intent)
        nb.setAutoCancel(true)

        val nm = context.getSystemService<NotificationManager>()
        nm?.notify(SgApp.NOTIFICATION_TRAKT_AUTH_ID, nb.build())
    }

    /**
     * Only removes the access token, but keeps the username.
     */
    private fun removeAccessToken() {
        hasCredentials = false
        setAccessToken(null)
    }

    /**
     * Removes the username and access token.
     */
    @Synchronized
    fun removeCredentials() {
        removeAccessToken()
        setUsername(null)
    }

    /**
     * Get the optional display name.
     */
    val displayName: String?
        get() = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_DISPLAYNAME, null)

    /**
     * Get the access token. Avoid keeping this in memory, maybe calling [hasCredentials]
     * is sufficient.
     */
    val accessToken: String?
        get() {
            val account = AccountUtils.getAccount(context) ?: return null
            val manager = AccountManager.get(context)
            return manager.getPassword(account)
        }

    /**
     * Stores the given access token.
     */
    @Synchronized
    fun storeAccessToken(accessToken: String) {
        require(!TextUtils.isEmpty(accessToken)) { "Access token is null or empty." }
        hasCredentials = setAccessToken(accessToken)
    }

    /**
     * Stores the given user name and display name.
     */
    @Synchronized
    fun storeUsername(username: String, displayname: String?): Boolean {
        require(!TextUtils.isEmpty(username)) { "Username is null or empty." }
        return (setUsername(username)
                && !TextUtils.isEmpty(displayname) && setDisplayname(displayname))
    }

    private fun setUsername(username: String?): Boolean {
        this.username = username
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_USERNAME, username)
            .commit()
    }

    private fun setDisplayname(displayname: String?): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY_DISPLAYNAME, displayname)
            .commit()
    }

    private fun setAccessToken(accessToken: String?): Boolean {
        var account = AccountUtils.getAccount(context)
        if (account == null) {
            // try to create a new account
            AccountUtils.createAccount(context)
        }

        account = AccountUtils.getAccount(context)
        if (account == null) {
            // give up
            return false
        }

        val manager = AccountManager.get(context)
        manager.setPassword(account, accessToken)

        return true
    }

    /**
     * Tries to refresh the current access token. Returns `false` on failure.
     */
    @Synchronized
    fun refreshAccessToken(trakt: TraktV2): Boolean {
        // do we even have a refresh token?
        val oldRefreshToken = TraktOAuthSettings.getRefreshToken(context)
        if (oldRefreshToken == null || oldRefreshToken.isEmpty()) {
            Timber.d("refreshAccessToken: no refresh token, give up.")
            return false
        }

        // try to get a new access token from trakt
        var accessToken: String? = null
        var refreshToken: String? = null
        var expiresIn: Long = -1
        try {
            val response = trakt.refreshAccessToken(oldRefreshToken)
            val token = response.body()
            if (response.isSuccessful && token != null) {
                accessToken = token.access_token
                refreshToken = token.refresh_token
                expiresIn = token.expires_in?.toLong() ?: 0
            } else {
                if (!SgTrakt.isUnauthorized(response)) {
                    Errors.logAndReport("refresh access token", response)
                }
            }
        } catch (e: IOException) {
            Errors.logAndReport("refresh access token", e)
        }

        // did we obtain all required data?
        if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken) || expiresIn < 1) {
            Timber.e("refreshAccessToken: failed.")
            return false
        }

        // store the new access token, refresh token and expiry date
        if (!setAccessToken(accessToken)
            || !TraktOAuthSettings.storeRefreshData(context, refreshToken!!, expiresIn)) {
            Timber.e("refreshAccessToken: saving failed")
            return false
        }

        Timber.d("refreshAccessToken: success.")
        return true
    }

    companion object {
        private const val KEY_USERNAME = "com.battlelancer.seriesguide.traktuser"
        private const val KEY_DISPLAYNAME = "com.battlelancer.seriesguide.traktuser.name"

        @SuppressLint("StaticFieldLeak")
        private var instance: TraktCredentials? = null

        @JvmStatic
        fun get(context: Context): TraktCredentials {
            // double-checked locking
            return instance ?: synchronized(this) {
                instance ?: TraktCredentials(context).also { instance = it }
            }
        }

        /**
         * Checks for existing trakt credentials. If there aren't any valid ones, launches the trakt
         * connect flow.
         *
         * @return **true** if credentials are valid, **false** if invalid and launching trakt
         * connect flow.
         */
        @JvmStatic
        fun ensureCredentials(context: Context): Boolean {
            if (!get(context).hasCredentials()) {
                // launch trakt connect flow
                context.startActivity(Intent(context, ConnectTraktActivity::class.java))
                return false
            }
            return true
        }
    }

}