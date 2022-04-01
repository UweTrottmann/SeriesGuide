package com.battlelancer.seriesguide.backend

import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.backend.CloudEndpointUtils.updateBuilder
import com.battlelancer.seriesguide.backend.HexagonAuthError.Companion.build
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.sync.NetworkJobProcessor
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReport
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReportHexagon
import com.battlelancer.seriesguide.util.isRetryError
import com.firebase.ui.auth.AuthUI
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.throwUnless
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uwetrottmann.seriesguide.backend.account.Account
import com.uwetrottmann.seriesguide.backend.episodes.Episodes
import com.uwetrottmann.seriesguide.backend.lists.Lists
import com.uwetrottmann.seriesguide.backend.movies.Movies
import com.uwetrottmann.seriesguide.backend.shows.Shows
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles credentials and services for interacting with Hexagon.
 */
@Singleton // needs global state for lastSignInCheck + to avoid rebuilding services
class HexagonTools @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Only checking once, assuming that if Play Services are missing or invalid this won't change.
     */
    val isGoogleSignInAvailable: Boolean by lazy {
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        // Return false only on non-resolvable errors. Firebase AuthUI can help resolve others.
        code != ConnectionResult.SERVICE_MISSING && code != ConnectionResult.SERVICE_INVALID
    }

    val firebaseSignInProviders: List<AuthUI.IdpConfig> by lazy {
        if (isGoogleSignInAvailable) {
            listOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
            )
        } else {
            listOf(AuthUI.IdpConfig.EmailBuilder().build())
        }
    }

    private val httpRequestInitializer by lazy { FirebaseHttpRequestInitializer() }
    private var lastSignInCheck: Long = 0

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with [HexagonSettings.isEnabled].
     */
    @get:Synchronized
    var showsService: Shows? = null
        get() {
            val requestInitializer = getHttpRequestInitializerIfSignedIn()
                ?: return null
            if (field == null) {
                val builder = Shows.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                field = updateBuilder(context, builder).build()
            }
            return field
        }
        private set

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with [HexagonSettings.isEnabled].
     */
    @get:Synchronized
    var episodesService: Episodes? = null
        get() {
            val requestInitializer = getHttpRequestInitializerIfSignedIn()
                ?: return null
            if (field == null) {
                val builder = Episodes.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                field = updateBuilder(context, builder).build()
            }
            return field
        }
        private set

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with [HexagonSettings.isEnabled].
     */
    @get:Synchronized
    var moviesService: Movies? = null
        get() {
            val requestInitializer = getHttpRequestInitializerIfSignedIn()
                ?: return null
            if (field == null) {
                val builder = Movies.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                field = updateBuilder(context, builder).build()
            }
            return field
        }
        private set

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @get:Synchronized
    var listsService: Lists? = null
        get() {
            val requestInitializer = getHttpRequestInitializerIfSignedIn()
                ?: return null
            if (field == null) {
                val builder = Lists.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
                field = updateBuilder(context, builder).build()
            }
            return field
        }
        private set

    /**
     * Creates and returns a new instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with [HexagonSettings.isEnabled].
     */
    @Synchronized
    fun buildAccountService(): Account? {
        val requestInitializer = getHttpRequestInitializerIfSignedIn()
            ?: return null
        val builder = Account.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer)
        return updateBuilder(context, builder).build()
    }

    /**
     * Enables Hexagon and saves account data. If there was no account or the email address
     * has changed re-sets sync state.
     *
     * @return `false` if sync state could not be reset or enabled state was not saved.
     */
    fun setAccountAndEnabled(firebaseUser: FirebaseUser): Boolean {
        val resetSyncState = HexagonSettings.getAccountName(context) != firebaseUser.email
        if (resetSyncState) {
            if (!HexagonSettings.resetSyncState(context)) {
                return false
            }
            // Avoid sending jobs if doing a full sync next.
            // Note: won't run if Trakt is still signed in.
            NetworkJobProcessor(context).removeObsoleteJobs(true)
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HexagonSettings.KEY_ENABLED, true)
                .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, false)
                .commit()) {
            return false
        }
        storeAccount(firebaseUser)
        return true
    }

    /**
     * Disables Hexagon and removes any account data.
     */
    fun removeAccountAndSetDisabled() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(HexagonSettings.KEY_ENABLED, false)
            .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, false)
            .apply()
        storeAccount(null)
    }

    /**
     * Get the Firebase user credentials to talk with Hexagon.
     *
     * Make sure to check [FirebaseHttpRequestInitializer.firebaseUser] is not null (the
     * account might have gotten signed out).
     *
     * @param checkSignInState If enabled, tries to silently sign in with Google. If it fails, sets
     * the [HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT] flag. If successful, clears the flag.
     */
    @Synchronized
    private fun getHttpRequestInitializer(checkSignInState: Boolean): FirebaseHttpRequestInitializer {
        if (checkSignInState) {
            checkSignInState()
        }
        return httpRequestInitializer
    }

    private fun getHttpRequestInitializerIfSignedIn(): HttpRequestInitializer? {
        val httpRequestInitializer = getHttpRequestInitializer(true)
        return if (httpRequestInitializer.firebaseUser == null) {
            null
        } else {
            httpRequestInitializer
        }
    }

    private fun checkSignInState() {
        if (httpRequestInitializer.firebaseUser != null && !isTimeForSignInStateCheck) {
            return
        }
        lastSignInCheck = SystemClock.elapsedRealtime()

        var account = FirebaseAuth.getInstance().currentUser
        if (account != null) {
            // still signed in
            httpRequestInitializer.firebaseUser = account
        } else {
            // try to silently sign in
            val signInTask = AuthUI.getInstance().silentSignIn(context, firebaseSignInProviders)
            try {
                val authResult = Tasks.await(signInTask)
                if (authResult?.user != null) {
                    Timber.i("%s: successful", ACTION_SILENT_SIGN_IN)
                    authResult.user.let {
                        account = it
                        httpRequestInitializer.firebaseUser = it
                    }
                } else {
                    logAndReport(
                        ACTION_SILENT_SIGN_IN,
                        HexagonAuthError(ACTION_SILENT_SIGN_IN, "FirebaseUser is null")
                    )
                }
            } catch (e: Exception) {
                if (e is InterruptedException) {
                    // Do not report thread interruptions, it's expected.
                    Timber.w(e, "Sign-in check interrupted")
                } else {
                    logAndReport(
                        ACTION_SILENT_SIGN_IN,
                        build(ACTION_SILENT_SIGN_IN, e)
                    )
                }
            }
        }

        val shouldFixAccount = account == null
        HexagonSettings.shouldValidateAccount(context, shouldFixAccount)
    }

    private val isTimeForSignInStateCheck: Boolean
        get() = lastSignInCheck + SIGN_IN_CHECK_INTERVAL_MS < SystemClock.elapsedRealtime()

    /**
     * Sets the account used for calls to Hexagon and saves the email address to display it in UI.
     */
    private fun storeAccount(firebaseUser: FirebaseUser?) {
        // store or remove account name in settings
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(HexagonSettings.KEY_ACCOUNT_NAME, firebaseUser?.email)
            .apply()

        // try to set or remove account on credential
        getHttpRequestInitializer(false).firebaseUser = firebaseUser
    }

    /**
     * Gets show by TMDB ID, or if not found and TVDB ID given, gets show by that.
     * Returns null value if not found.
     */
    fun getShow(showTmdbId: Int, showTvdbId: Int?): Result<SgCloudShow?, HexagonError> {
        val showsService = showsService
            ?: return Err(HexagonStop)
        return runCatching {
            var showOrNull = showsService.sgShow
                .setShowTmdbId(showTmdbId)
                .execute()
            if (showOrNull == null && showTvdbId != null && showTvdbId > 0) {
                // Not found using TMDB ID, try with legacy TVDB ID.
                val legacyShowOrNull = showsService.show
                    .setShowTvdbId(showTvdbId)
                    .execute()
                if (legacyShowOrNull != null) {
                    showOrNull = SgCloudShow()
                    showOrNull.isFavorite = legacyShowOrNull.isFavorite
                    showOrNull.isHidden = legacyShowOrNull.isHidden
                    showOrNull.notify = legacyShowOrNull.notify
                }
            }
            showOrNull
        }.throwUnless {
            // Note: JSON parser may throw IllegalArgumentException.
            it is IOException || it is IllegalArgumentException
        }.mapError {
            logAndReportHexagon("h: get show", it)
            if (it.isRetryError()) HexagonRetry else HexagonStop
        }
    }

    companion object {
        private const val ACTION_SILENT_SIGN_IN = "silent sign-in"
        private val JSON_FACTORY: JsonFactory = GsonFactory()
        private val HTTP_TRANSPORT: HttpTransport = NetHttpTransport()
        private const val SIGN_IN_CHECK_INTERVAL_MS = 5 * DateUtils.MINUTE_IN_MILLIS
    }
}

sealed class HexagonError

/**
 * The API request might succeed if tried again after a brief delay
 * (e.g. time outs or other temporary network issues).
 */
object HexagonRetry : HexagonError()

/**
 * The API request is unlikely to succeed if retried, at least right now
 * (e.g. API bugs or changes).
 */
object HexagonStop : HexagonError()
