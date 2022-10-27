package com.battlelancer.seriesguide.traktapi

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReportNoBend
import com.battlelancer.seriesguide.util.Utils
import timber.log.Timber

/**
 * Base class to create an OAuth 2.0 authorization flow using the browser, offering fallback to an
 * embedded [android.webkit.WebView].
 */
abstract class BaseOAuthActivity : BaseActivity() {

    private var webview: WebView? = null
    private lateinit var buttonContainer: View
    private lateinit var progressBar: View
    private lateinit var textViewMessage: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oauth)
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

    private fun bindViews() {
        buttonContainer = findViewById(R.id.containerOauthButtons)
        progressBar = findViewById(R.id.progressBarOauth)
        textViewMessage = buttonContainer.findViewById(R.id.textViewOauthMessage)

        // set up buttons (can be used if browser launch fails or user comes back without code)
        findViewById<Button>(R.id.buttonOauthBrowser).setOnClickListener { launchBrowser() }
        findViewById<Button>(R.id.buttonOauthWebView).setOnClickListener { activateWebView() }

        activateFallbackButtons()
        setMessage(null)
    }

    private fun launchBrowser() {
        val authorizationUrl = authorizationUrl
        if (authorizationUrl != null) {
            Utils.launchWebsite(this, authorizationUrl)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
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
        webview?.visibility = View.GONE
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun activateWebView() {
        buttonContainer.visibility = View.GONE

        // Inflate the WebView on demand.
        val webview = findViewById(R.id.webView)
            ?: try {
                val container = findViewById<FrameLayout>(R.id.frameLayoutOauth)
                LayoutInflater.from(container.context)
                    .inflate(R.layout.view_webview, container, true)
                findViewById<WebView>(R.id.webView)
            } catch (e: Exception) {
                // There are various crashes where inflating fails due to a
                // "Failed to load WebView provider: No WebView installed" exception.
                // The most reasonable explanation is that the WebView is getting updated right
                // when we want to inflate it.
                // So just finish the activity and make the user open it again.
                logAndReportNoBend("Inflate WebView", e)
                finish()
                return
            }
        this.webview = webview
        webview.also {
            it.visibility = View.VISIBLE
            it.webViewClient = webViewClient
            it.settings.javaScriptEnabled = true
        }

        // Clear all previous sign-in state.
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        webview.clearCache(true)

        // Load the authorization page.
        Timber.d("Initiating authorization request...")
        val authUrl = authorizationUrl
        if (authUrl != null) {
            webview.loadUrl(authUrl)
        }
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

    protected var webViewClient: WebViewClient = object : WebViewClient() {
        override fun onReceivedError(
            view: WebView, errorCode: Int, description: String,
            failingUrl: String
        ) {
            Timber.e("WebView error: %s %s", errorCode, description)
            activateFallbackButtons()
            setMessage("$authErrorMessage\n\n($errorCode $description)")
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
            if (url != null && url.startsWith(OAUTH_CALLBACK_URL_CUSTOM)) {
                val uri = Uri.parse(url)
                fetchTokensAndFinish(uri.getQueryParameter("code"), uri.getQueryParameter("state"))
                return true
            }
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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