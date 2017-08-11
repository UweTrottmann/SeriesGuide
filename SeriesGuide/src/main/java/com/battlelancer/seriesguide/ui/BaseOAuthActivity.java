package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

/**
 * Base class to create an OAuth 2.0 authorization flow using the browser, offering fallback to an
 * embedded {@link android.webkit.WebView}.
 */
public abstract class BaseOAuthActivity extends BaseActivity {

    /** Pass with true to not auto launch the external browser, display default error message. */
    public static final String EXTRA_KEY_IS_RETRY = "isRetry";

    /** Needs to match with the scheme registered in the manifest. */
    private static final String OAUTH_URI_SCHEME = "sgoauth";
    public static final String OAUTH_CALLBACK_URL_CUSTOM = OAUTH_URI_SCHEME + "://callback";

    private WebView webview;
    private View buttonContainer;
    private View progressBar;
    private TextView textViewMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth);
        setupActionBar();

        setupViews();

        if (handleAuthIntent(getIntent())) {
            return;
        }

        boolean isRetry = getIntent().getBooleanExtra(EXTRA_KEY_IS_RETRY, false);
        if (isRetry) {
            setMessage(getAuthErrorMessage());
        }

        if (savedInstanceState == null && !isRetry) {
            // try to launch external browser with OAuth page
            launchBrowser();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /**
         * Force the text-to-speech accessibility Javascript plug-in service on Android 4.2.2 to
         * get shutdown, to avoid leaking its context.
         *
         * http://stackoverflow.com/a/18798305/1000543
         */
        if (webview != null) {
            webview.getSettings().setJavaScriptEnabled(false);
            // remove client to avoid callbacks to non-existent views
            webview.setWebViewClient(null);
            webview = null;
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        webview = (WebView) findViewById(R.id.webView);
        buttonContainer = findViewById(R.id.containerOauthButtons);
        progressBar = findViewById(R.id.progressBarOauth);
        textViewMessage = buttonContainer.findViewById(R.id.textViewOauthMessage);

        // setup buttons (can be used if browser launch fails or user comes back without code)
        Button buttonBrowser = (Button) findViewById(R.id.buttonOauthBrowser);
        buttonBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchBrowser();
            }
        });
        Button buttonWebView = (Button) findViewById(R.id.buttonOauthWebView);
        buttonWebView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateWebView();
            }
        });

        activateFallbackButtons();
        setMessage(null);
    }

    private void launchBrowser() {
        String authorizationUrl = getAuthorizationUrl();
        if (authorizationUrl != null) {
            Utils.launchWebsite(this, authorizationUrl, "OAuth", "Launch browser for OAuth");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleAuthIntent(intent);
    }

    private boolean handleAuthIntent(Intent intent) {
        // handle auth callback from external browser
        Uri callbackUri = intent.getData();
        if (callbackUri == null || !callbackUri.getScheme().equals(OAUTH_URI_SCHEME)) {
            return false;
        }
        fetchTokensAndFinish(callbackUri.getQueryParameter("code"),
                callbackUri.getQueryParameter("state"));
        return true;
    }

    protected void activateFallbackButtons() {
        webview.setVisibility(View.GONE);
        buttonContainer.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void activateWebView() {
        buttonContainer.setVisibility(View.GONE);
        webview.setVisibility(View.VISIBLE);

        webview.setWebViewClient(webViewClient);
        webview.getSettings().setJavaScriptEnabled(true);

        // make sure we start fresh
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        webview.clearCache(true);

        // load the authorization page
        Timber.d("Initiating authorization request...");
        String authUrl = getAuthorizationUrl();
        if (authUrl != null) {
            webview.loadUrl(authUrl);
        }
    }

    protected void setMessage(String message) {
        setMessage(message, false);
    }

    protected void setMessage(String message, boolean progressVisible) {
        if (message == null) {
            textViewMessage.setVisibility(View.GONE);
        } else {
            textViewMessage.setVisibility(View.VISIBLE);
            textViewMessage.setText(message);
        }
        progressBar.setVisibility(progressVisible ? View.VISIBLE : View.GONE);
    }

    protected WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            Timber.e("WebView error: %s %s", errorCode, description);
            activateFallbackButtons();
            setMessage(getAuthErrorMessage() + "\n\n(" + errorCode + " " + description + ")");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null && url.startsWith(OAUTH_CALLBACK_URL_CUSTOM)) {
                Uri uri = Uri.parse(url);
                fetchTokensAndFinish(uri.getQueryParameter("code"), uri.getQueryParameter("state"));
                return true;
            }
            return false;
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Return the url of the OAuth authorization page.
     */
    @Nullable
    protected abstract String getAuthorizationUrl();

    /**
     * Return an error message displayed if authorization fails at any point.
     */
    protected abstract String getAuthErrorMessage();

    /**
     * Called with the OAuth auth code and state retrieved from the {@link #getAuthorizationUrl()}
     * once the user has authorized us. If state was sent, ensure it matches. Then retrieve the
     * OAuth tokens with the auth code.
     */
    protected abstract void fetchTokensAndFinish(@Nullable String authCode, @Nullable String state);
}
