
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Displays the SeriesGuide online help page.
 */
public class HelpActivity extends BaseActivity {

    private static final String TAG = "Help";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle arg0) {
        // webview uses a progress bar
        requestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(arg0);

        WebView webview = new WebView(this);
        setContentView(webview);

        setupActionBar();

        setSupportProgressBarVisibility(true);

        final BaseActivity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                /*
                 * Activities and WebViews measure progress with different
                 * scales. The progress meter will automatically disappear when
                 * we reach 100%.
                 */
                activity.setSupportProgress(progress * 1000);
            }
        });
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(getString(R.string.help_url));
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.help);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.help_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (itemId == R.id.menu_feedback) {
            fireTrackerEvent("Feedback");

            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                    SeriesGuidePreferences.SUPPORT_MAIL
            });
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "SeriesGuide " + Utils.getVersion(this) + " Feedback");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(intent, getString(R.string.feedback)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
