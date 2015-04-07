
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;
import org.apache.http.protocol.HTTP;

/**
 * Displays the SeriesGuide online help page.
 */
public class HelpActivity extends BaseActivity {

    private static final String TAG = "Help";
    private WebView webview;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_webview);
        setupActionBar();

        webview = (WebView) findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(webViewClient);
        webview.loadUrl(getString(R.string.help_url));
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

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null && !url.startsWith(getString(R.string.help_url))) {
                // launch browser when leaving help page
                Utils.launchWebsite(view.getContext(), url, TAG, "Non-help page");
                return true;
            }
            return false;
        }
    };

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.help);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (itemId == R.id.menu_action_help_open_browser) {
            openInBrowser();
            return true;
        }
        if (itemId == R.id.menu_action_help_send_feedback) {
            sendEmail();
            fireTrackerEvent("Feedback");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openInBrowser() {
        Utils.launchWebsite(this, getString(R.string.help_url), TAG, "Open In Browser");
    }

    private void sendEmail() {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType(HTTP.PLAIN_TEXT_TYPE);
        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                SeriesGuidePreferences.SUPPORT_MAIL
        });
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "SeriesGuide " + Utils.getVersion(this) + " Feedback");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

        Intent chooser = Intent.createChooser(intent, getString(R.string.feedback));
        Utils.tryStartActivity(this, chooser, true);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
