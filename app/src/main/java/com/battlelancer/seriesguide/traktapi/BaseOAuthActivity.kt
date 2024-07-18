// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.WebTools

/**
 * Base class to create an OAuth 2.0 authorization flow using the browser, uses a custom tab
 * (which falls back to opening the default browser).
 *
 * Launches the browser directly when created. Shows a progress bar and status or error message and
 * button to manually launch browser.
 */
abstract class BaseOAuthActivity : BaseActivity() {

    private lateinit var buttonContainer: View
    private lateinit var progressBar: View
    private lateinit var textViewMessage: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutOauth))
        setupActionBar()

        bindViews()

        if (handleAuthIntent(intent)) {
            return
        }

        val isRetry = intent.getBooleanExtra(EXTRA_KEY_IS_RETRY, false)
        if (isRetry) {
            setMessage(authErrorMessage)
        }

        if (savedInstanceState == null && !isRetry) {
            // try to launch external browser with OAuth page
            launchBrowser()
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun bindViews() {
        buttonContainer = findViewById(R.id.containerOauthButtons)
        ThemeUtils.applyBottomPaddingForNavigationBar(buttonContainer)
        progressBar = findViewById(R.id.progressBarOauth)
        textViewMessage = buttonContainer.findViewById(R.id.textViewOauthMessage)

        // Set up button (can be used if browser launch fails or user comes back without code)
        findViewById<Button>(R.id.buttonOauthBrowser).setOnClickListener { launchBrowser() }

        activateFallbackButtons()
        setMessage(null)
    }

    private fun launchBrowser() {
        val authorizationUrl = authorizationUrl
        if (authorizationUrl != null) {
            WebTools.openInCustomTab(this, authorizationUrl)
        }
    }

    private fun handleAuthIntent(intent: Intent): Boolean {
        // handle auth callback from external browser
        val callbackUri = intent.data
        if (callbackUri == null || OAUTH_URI_SCHEME != callbackUri.scheme) {
            return false
        }
        fetchTokensAndFinish(
            callbackUri.getQueryParameter("code"),
            callbackUri.getQueryParameter("state")
        )
        return true
    }

    protected fun activateFallbackButtons() {
        buttonContainer.visibility = View.VISIBLE
    }

    protected fun setMessage(message: CharSequence?) {
        setMessage(message, false)
    }

    protected fun setMessage(message: CharSequence?, progressVisible: Boolean) {
        if (message == null) {
            textViewMessage.visibility = View.GONE
        } else {
            textViewMessage.visibility = View.VISIBLE
            textViewMessage.text = message
        }
        progressBar.visibility = if (progressVisible) View.VISIBLE else View.GONE
    }

    /**
     * Return the url of the OAuth authorization page.
     */
    protected abstract val authorizationUrl: String?

    /**
     * Return an error message displayed if authorization fails at any point.
     */
    protected abstract val authErrorMessage: String

    /**
     * Called with the OAuth auth code and state retrieved from the [authorizationUrl]
     * once the user has authorized us. If state was sent, ensure it matches. Then retrieve the
     * OAuth tokens with the auth code.
     */
    protected abstract fun fetchTokensAndFinish(authCode: String?, state: String?)

    companion object {
        /** Pass with true to not auto launch the external browser, display default error message. */
        const val EXTRA_KEY_IS_RETRY = "isRetry"

        /** Needs to match with the scheme registered in the manifest. */
        private const val OAUTH_URI_SCHEME = "sgoauth"
        const val OAUTH_CALLBACK_URL_CUSTOM = "$OAUTH_URI_SCHEME://callback"
    }
}