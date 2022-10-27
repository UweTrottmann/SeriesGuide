package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Errors;
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

    @Nullable private WebView webview;
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
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        buttonContainer = findViewById(R.id.containerOauthButtons);
        progressBar = findViewById(R.id.progressBarOauth);
        textViewMessage = buttonContainer.findViewById(R.id.textViewOauthMessage);

        // setup buttons (can be used if browser launch fails or user comes back without code)
        Button buttonBrowser = findViewById(R.id.buttonOauthBrowser);
        buttonBrowser.setOnClickListener(v -> launchBrowser());
        Button buttonWebView = findViewById(R.id.buttonOauthWebView);
        buttonWebView.setOnClickListener(v -> activateWebView());

        activateFallbackButtons();
        setMessage(null);
    }

    private void launchBrowser() {
        String authorizationUrl = getAuthorizationUrl();
        if (authorizationUrl != null) {
            Utils.launchWebsite(this, authorizationUrl);
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
        if (callbackUri == null || !OAUTH_URI_SCHEME.equals(callbackUri.getScheme())) {
            return false;
        }
        fetchTokensAndFinish(callbackUri.getQueryParameter("code"),
                callbackUri.getQueryParameter("state"));
        return true;
    }

    protected void activateFallbackButtons() {
        buttonContainer.setVisibility(View.VISIBLE);
        if (webview != null) {
            webview.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void activateWebView() {
        buttonContainer.setVisibility(View.GONE);

        // Inflate the WebView on demand.
        WebView webview = findViewById(R.id.webView);
        if (webview == null) {
            FrameLayout container = findViewById(R.id.frameLayoutOauth);
            try {
                LayoutInflater.from(container.getContext())
                        .inflate(R.layout.view_webview, container, true);
                webview = findViewById(R.id.webView);
            } catch (Exception e) {
                // There are various crashes where inflating fails due to a
                // "Failed to load WebView provider: No WebView installed" exception.
                // The most reasonable explanation is that the WebView is getting updated right
                // when we want to inflate it.
                // So just finish the activity and make the user open it again.
                Errors.logAndReportNoBend("Inflate WebView", e);
                finish();
                return;
            }
        }
        this.webview = webview;

        webview.setVisibility(View.VISIBLE);

        webview.setWebViewClient(webViewClient);
        webview.getSettings().setJavaScriptEnabled(true);

        // Clear all previous sign-in state.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        webview.clearCache(true);

        // Load the authorization page.
        Timber.d("Initiating authorization request...");
        String authUrl = getAuthorizationUrl();
        if (authUrl != null) {
            webview.loadUrl(authUrl);
        }
    }

    protected void setMessage(CharSequence message) {
        setMessage(message, false);
    }

    protected void setMessage(CharSequence message, boolean progressVisible) {
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
