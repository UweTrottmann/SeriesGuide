/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import timber.log.Timber;

/**
 * Base class to create an OAuth 2.0 authorization flow using an embedded {@link
 * android.webkit.WebView}.
 */
public abstract class BaseOAuthActivity extends BaseActivity {

    public static final String OAUTH_CALLBACK_URL_LOCALHOST = "http://localhost";
    private WebView webview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        setupActionBar();

        webview = (WebView) findViewById(R.id.webView);
        setupViews(webview);
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
            webview = null;
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void setupViews(WebView webview) {
        webview.setWebViewClient(webViewClient);
        webview.getSettings().setJavaScriptEnabled(true);

        // make sure we start fresh
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        webview.clearCache(true);

        // load the authorization page
        Timber.d("Initiating authorization request...");
        String authUrl = getAuthorizationUrl();
        if (TextUtils.isEmpty(authUrl)) {
            Toast.makeText(this, getAuthErrorMessage(), Toast.LENGTH_LONG).show();
            finish();
        } else {
            webview.loadUrl(authUrl);
        }
    }

    protected WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            Toast.makeText(BaseOAuthActivity.this, getAuthErrorMessage() + " " + description,
                    Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null && url.startsWith(OAUTH_CALLBACK_URL_LOCALHOST)) {
                Uri uri = Uri.parse(url);

                fetchTokens(uri.getQueryParameter("code"), uri.getQueryParameter("state"));

                finish();
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
    protected abstract void fetchTokens(@Nullable String authCode, @Nullable String state);
}
