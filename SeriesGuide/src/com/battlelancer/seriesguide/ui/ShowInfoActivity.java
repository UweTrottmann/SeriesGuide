
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class ShowInfoActivity extends BaseActivity {
    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

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
        AnalyticsUtils.getInstance(this).trackEvent("ShowInfo", "Click", label, 0);
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
        AnalyticsUtils.getInstance(this).trackPageView("/ShowInfo");
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
        switch (item.getItemId()) {
            case android.R.id.home: {
                navigateToOverview(getShowId());
                return true;
            }
            case R.id.menu_rate_trakt: {
                TraktRateDialogFragment newFragment = TraktRateDialogFragment
                        .newInstance(getShowId());
                newFragment.show(getSupportFragmentManager(), "traktratedialog");
                return true;
            }
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
        TextView airstime = (TextView) findViewById(R.id.TextViewShowInfoAirtime);
        TextView network = (TextView) findViewById(R.id.TextViewShowInfoNetwork);
        TextView status = (TextView) findViewById(R.id.TextViewShowInfoStatus);

        final Series show = DBUtils.getShow(this, String.valueOf(getShowId()));
        if (show == null) {
            finish();
        }

        // Name
        seriesname.setText(show.getSeriesName());

        // Overview
        if (show.getOverview().length() == 0) {
            overview.setText("");
        } else {
            overview.setText(show.getOverview());
        }

        // Airtimes
        if (show.getAirsDayOfWeek().length() == 0 || show.getAirsTime() == -1) {
            airstime.setText(getString(R.string.show_noairtime));
        } else {
            String[] values = Utils.parseMillisecondsToTime(show.getAirsTime(),
                    show.getAirsDayOfWeek(), getApplicationContext());
            airstime.setText(getString(R.string.show_airs) + " " + values[1] + " " + values[0]);
        }

        // Network
        if (show.getNetwork().length() == 0) {
            network.setText("");
        } else {
            network.setText(getString(R.string.show_network) + " " + show.getNetwork());
        }

        // Running state
        if (show.getStatus() == 1) {
            status.setTextColor(Color.GREEN);
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

        // IMDb button
        View imdbButton = (View) findViewById(R.id.buttonShowInfoIMDB);
        final String imdbid = show.getImdbId();
        if (imdbButton != null) {
            imdbButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    fireTrackerEvent("IMDb");

                    if (imdbid.length() != 0) {
                        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///title/"
                                + imdbid + "/"));
                        try {
                            startActivity(myIntent);
                        } catch (ActivityNotFoundException e) {
                            myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(IMDB_TITLE_URL
                                    + imdbid));
                            startActivity(myIntent);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.show_noimdbentry), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        // TVDb button
        View tvdbButton = (View) findViewById(R.id.buttonTVDB);
        final String tvdbId = show.getId();
        if (tvdbButton != null) {
            tvdbButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    fireTrackerEvent("TVDb");
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
                TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(
                        show.getSeriesName(), Integer.valueOf(tvdbId));

                newFragment.show(getSupportFragmentManager(), "shouts-dialog");
            }
        });

        // Share intent
        mShareIntentBuilder = ShareCompat.IntentBuilder
                .from(this)
                .setChooserTitle(R.string.share)
                .setText(
                        getString(R.string.share_checkout) + " \"" + show.getSeriesName()
                                + "\" via @SeriesGuide " + ShowInfoActivity.IMDB_TITLE_URL + imdbid)
                .setType("text/plain");

        // Poster
        final ImageView poster = (ImageView) findViewById(R.id.ImageViewShowInfoPoster);
        ImageProvider.getInstance(this).loadPoster(poster, show.getPoster(), false);

        // trakt ratings
        TraktSummaryTask task = new TraktSummaryTask(this, findViewById(R.id.ratingbar))
                .show(tvdbId);
        Utils.executeAsyncTask(task, new Void[] {
            null
        });
    }
}
