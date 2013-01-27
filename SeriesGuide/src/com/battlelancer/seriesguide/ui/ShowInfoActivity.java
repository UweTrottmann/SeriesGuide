/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays detailed information about a show.
 */
public class ShowInfoActivity extends BaseActivity {

    private static final String TAG = "ShowInfoActivity";

    private IntentBuilder mShareIntentBuilder;

    public interface InitBundle {
        String SHOW_TVDBID = "tvdbid";
    }

    /**
     * Google Analytics helper method for easy event tracking.
     * 
     * @param label
     */
    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent("ShowInfo", "Click", label, (long) 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_info);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.context_showinfo));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        fillData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.showinfo_menu, menu);

        MenuItem shareItem = menu.findItem(R.id.menu_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem
                .getActionProvider();
        shareActionProvider.setShareIntent(mShareIntentBuilder.getIntent());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent intent = new Intent(this, OverviewActivity.class);
            intent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, getShowId());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (itemId == R.id.menu_rate_trakt) {
            TraktRateDialogFragment newFragment = TraktRateDialogFragment
                    .newInstance(getShowId());
            newFragment.show(getSupportFragmentManager(), "traktratedialog");
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            ListsDialogFragment.showListsDialog(String.valueOf(getShowId()), 1,
                    getSupportFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected int getShowId() {
        return getIntent().getExtras().getInt(InitBundle.SHOW_TVDBID);
    }

    @TargetApi(11)
    private void fillData() {
        TextView seriesname = (TextView) findViewById(R.id.title);
        TextView overview = (TextView) findViewById(R.id.TextViewShowInfoOverview);
        TextView info = (TextView) findViewById(R.id.showInfo);
        TextView status = (TextView) findViewById(R.id.showStatus);

        final Series show = DBUtils.getShow(this, String.valueOf(getShowId()));
        if (show == null) {
            finish();
            return;
        }

        // Name
        seriesname.setText(show.getTitle());

        // Overview
        if (show.getOverview().length() == 0) {
            overview.setText("");
        } else {
            overview.setText(show.getOverview());
        }

        // air time
        StringBuilder infoText = new StringBuilder();
        if (show.getAirsDayOfWeek().length() == 0 || show.getAirsTime() == -1) {
            infoText.append(getString(R.string.show_noairtime));
        } else {
            String[] values = Utils.parseMillisecondsToTime(show.getAirsTime(),
                    show.getAirsDayOfWeek(), getApplicationContext());
            infoText.append(values[1]).append(" ").append(values[0]);
        }
        // network
        if (show.getNetwork().length() != 0) {
            infoText.append(" ").append(getString(R.string.show_network)).append(" ")
                    .append(show.getNetwork());
        }
        info.setText(infoText);

        // Running state
        if (show.getStatus() == 1) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.textColorSgGreen,
                    outValue, true);
            status.setTextColor(getResources().getColor(outValue.resourceId));
            status.setText(getString(R.string.show_isalive));
        } else if (show.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // first airdate
        long airtime = Utils.buildEpisodeAirtime(show.getFirstAired(), show.getAirsTime());
        Utils.setValueOrPlaceholder(findViewById(R.id.TextViewShowInfoFirstAirdate),
                Utils.formatToDate(airtime, this));

        // Others
        Utils.setValueOrPlaceholder(findViewById(R.id.TextViewShowInfoActors),
                Utils.splitAndKitTVDBStrings(show.getActors()));
        Utils.setValueOrPlaceholder(findViewById(R.id.TextViewShowInfoContentRating),
                show.getContentRating());
        Utils.setValueOrPlaceholder(findViewById(R.id.TextViewShowInfoGenres),
                Utils.splitAndKitTVDBStrings(show.getGenres()));
        Utils.setValueOrPlaceholder(findViewById(R.id.TextViewShowInfoRuntime), show.getRuntime()
                + " " + getString(R.string.show_airtimeunit));

        // TVDb rating
        String ratingText = show.getRating();
        if (ratingText != null && ratingText.length() != 0) {
            RatingBar ratingBar = (RatingBar) findViewById(R.id.bar);
            ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
            TextView rating = (TextView) findViewById(R.id.value);
            rating.setText(ratingText + "/10");
        }

        // Last edit date
        TextView lastEdit = (TextView) findViewById(R.id.lastEdit);
        long lastEditRaw = show.getLastEdit();
        if (lastEditRaw > 0) {
            lastEdit.setText(DateUtils.formatDateTime(this, lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            lastEdit.setText(R.string.unknown);
        }

        // Google Play button
        View playButton = findViewById(R.id.buttonGooglePlay);
        Utils.setUpGooglePlayButton(show.getTitle(), playButton, TAG);

        // Amazon button
        View amazonButton = findViewById(R.id.buttonAmazon);
        Utils.setUpAmazonButton(show.getTitle(), amazonButton, TAG);

        // IMDb button
        View imdbButton = (View) findViewById(R.id.buttonShowInfoIMDB);
        final String imdbId = show.getImdbId();
        Utils.setUpImdbButton(imdbId, imdbButton, TAG, this);

        // TVDb button
        View tvdbButton = (View) findViewById(R.id.buttonTVDB);
        final String tvdbId = show.getId();
        if (tvdbButton != null) {
            tvdbButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    fireTrackerEvent("Show TVDb page");
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TVDB_SHOW_URL
                            + tvdbId));
                    startActivity(i);
                }
            });
        }

        // Shout button
        findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ShowInfoActivity.this, TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundle(getShowId(),
                        0, 0, show.getTitle()));
                startActivity(i);
                fireTrackerEvent("Show Trakt Shouts");
            }
        });

        // Share intent
        mShareIntentBuilder = ShareCompat.IntentBuilder
                .from(this)
                .setChooserTitle(R.string.share)
                .setText(
                        getString(R.string.share_checkout) + " \"" + show.getTitle()
                                + "\" " + Utils.IMDB_TITLE_URL + imdbId)
                .setType("text/plain");

        // Poster
        final ImageView poster = (ImageView) findViewById(R.id.ImageViewShowInfoPoster);
        ImageProvider.getInstance(this).loadImage(poster, show.getPoster(), false);

        // trakt ratings
        TraktSummaryTask task = new TraktSummaryTask(this, findViewById(R.id.ratingbar))
                .show(tvdbId);
        AndroidUtils.executeAsyncTask(task, new Void[] {
                null
        });
    }
}
