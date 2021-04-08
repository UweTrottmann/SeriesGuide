package com.battlelancer.seriesguide.traktapi

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.text.HtmlCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.util.Errors
import timber.log.Timber
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Starts a Trakt OAuth 2.0 authorization flow using the default browser
 * or an embedded [android.webkit.WebView] as a fallback.
 */
class TraktAuthActivity : BaseOAuthActivity() {

    private var state: String? = null
    private val model by viewModels<TraktAuthActivityModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.connectResult.observe(this, {
            handleTraktConnectResult(it)
        })

        if (savedInstanceState != null) {
            // restore state on recreation
            // (e.g. if launching external browser dropped us out of memory)
            state = savedInstanceState.getString(KEY_STATE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STATE, state)
    }

    override fun getAuthorizationUrl(): String? {
        val state = BigInteger(130, SecureRandom()).toString(32).also {
            this.state = it
        }
        return getServicesComponent(this).trakt().buildAuthorizationUrl(state)
    }

    override fun getAuthErrorMessage(): String {
        return getString(R.string.trakt_error_credentials)
    }

    override fun fetchTokensAndFinish(authCode: String?, state: String?) {
        activateFallbackButtons()

        if (model.connectInProgress) {
            // connect task is still running
            setMessage(getString(R.string.waitplease), true)
            return
        }

        // if state does not match what we sent, drop the auth code
        val currentState = this.state
        if (currentState == null || currentState != state) {
            // log trakt OAuth failures
            Errors.logAndReportNoBend(
                ACTION_FETCHING_TOKENS,
                TraktOAuthError(ACTION_FETCHING_TOKENS, ERROR_DESCRIPTION_STATE_MISMATCH)
            )
            setMessage(
                authErrorMessage + if (currentState == null) {
                    "\n\n(State is null.)"
                } else {
                    "\n\n(State does not match. Cross-site request forgery detected.)"
                }
            )
            return
        }

        if (authCode.isNullOrEmpty()) {
            // no valid auth code, remain in activity and show fallback buttons
            Timber.e("Failed because no auth code returned.")
            setMessage("$authErrorMessage\n\n(No auth code returned.)")
            return
        }

        // fetch access token with given OAuth auth code
        setMessage(getString(R.string.waitplease), true)
        model.connectTrakt(authCode)
    }

    private fun handleTraktConnectResult(event: TraktAuthActivityModel.ConnectResult) {
        val resultCode = event.code
        if (resultCode == TraktResult.SUCCESS) {
            // if we got here, looks like credentials were stored successfully
            // trigger a sync, notifies user via toast
            SgSyncAdapter.requestSyncDeltaImmediate(this, true)
            finish()
            return
        }

        // handle errors
        var errorText: CharSequence = when (resultCode) {
            TraktResult.OFFLINE -> getString(R.string.offline)
            TraktResult.API_ERROR -> getString(
                R.string.api_error_generic,
                getString(R.string.trakt)
            )
            TraktResult.ACCOUNT_LOCKED -> getString(R.string.trakt_error_account_locked)
            TraktResult.AUTH_ERROR, TraktResult.ERROR -> getString(R.string.trakt_error_credentials)
            else -> getString(R.string.trakt_error_credentials)
        }

        if (event.debugMessage != null) {
            errorText = HtmlCompat.fromHtml(
                "<p>$errorText</p><p><i>${event.debugMessage}</i></p>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }

        setMessage(errorText)
        activateFallbackButtons()
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val ACTION_FETCHING_TOKENS = "fetching tokens"
        private const val ERROR_DESCRIPTION_STATE_MISMATCH =
            "invalid_state, State is null or does not match."
    }
}