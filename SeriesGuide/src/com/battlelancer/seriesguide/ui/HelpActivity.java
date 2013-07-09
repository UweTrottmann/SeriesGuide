
package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays the seriesguide online help page.
 */
public class HelpActivity extends BaseActivity {

    private static final String TAG = "Help";
    private WebView mWebview;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle arg0) {
        // webview uses a progress bar
        requestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(arg0);

        mWebview = new WebView(this);
        getMenu().setContentView(mWebview);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(R.string.help);

        setSupportProgressBarVisibility(true);

        final BaseActivity activity = this;
        mWebview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                /*
                 * Activities and WebViews measure progress with different
                 * scales. The progress meter will automatically disappear when
                 * we reach 100%.
                 */
                activity.setSupportProgress(progress * 1000);
            }
        });
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.loadUrl(getString(R.string.help_url));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.help_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
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
    };

    private void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Action Item", label, (long) 0);
    }
}
