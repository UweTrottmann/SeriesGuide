
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.thetvdbapi.ImageCache;
import com.battlelancer.thetvdbapi.Series;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class ShowInfo extends BaseActivity {
    public static final String IMDB_TITLE_URL = "http://imdb.com/title/";

    private ImageCache imageCache;

    private String seriesid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_info);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.context_showinfo));
        actionBar.setDisplayShowTitleEnabled(true);

        imageCache = ((SeriesGuideApplication) getApplication()).getImageCache();

        Bundle extras = getIntent().getExtras();
        seriesid = extras.getString(Shows._ID);

        fillData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/ShowInfo");
    }

    private void fillData() {
        TextView seriesname = (TextView) findViewById(R.id.title);
        TextView overview = (TextView) findViewById(R.id.TextViewShowInfoOverview);
        TextView actors = (TextView) findViewById(R.id.TextViewShowInfoActors);
        TextView airstime = (TextView) findViewById(R.id.TextViewShowInfoAirtime);
        TextView contentrating = (TextView) findViewById(R.id.TextViewShowInfoContentRating);
        TextView firstaired = (TextView) findViewById(R.id.TextViewShowInfoFirstAirdate);
        TextView genres = (TextView) findViewById(R.id.TextViewShowInfoGenres);
        TextView network = (TextView) findViewById(R.id.TextViewShowInfoNetwork);
        TextView rating = (TextView) findViewById(R.id.value);
        TextView runtime = (TextView) findViewById(R.id.TextViewShowInfoRuntime);
        TextView status = (TextView) findViewById(R.id.TextViewShowInfoStatus);
        ImageView showart = (ImageView) findViewById(R.id.ImageViewShowInfoPoster);
        View showInIMDB = (View) findViewById(R.id.buttonShowInfoIMDB);

        final Series show = SeriesDatabase.getShow(this, seriesid);
        if (show == null) {
            finish();
        }

        // Name
        seriesname.setText(show.getSeriesName());

        // Overview
        if (show.getOverview().length() == 0) {
            overview.setText(getString(R.string.show_pleaseupdate));
        } else {
            overview.setText(show.getOverview());
        }

        // Airtimes
        if (show.getAirsDayOfWeek().length() == 0 || show.getAirsTime() == -1) {
            airstime.setText(getString(R.string.show_noairtime));
        } else {
            String[] values = SeriesGuideData.parseMillisecondsToTime(show.getAirsTime(),
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

        // Others
        actors.setText(getString(R.string.show_actors) + " "
                + SeriesGuideData.splitAndKitTVDBStrings(show.getActors()));
        contentrating.setText(getString(R.string.show_contentrating) + " "
                + show.getContentRating());
        firstaired.setText(getString(R.string.show_firstaired)
                + " "
                + SeriesGuideData.parseDateToLocal(show.getFirstAired(), show.getAirsTime(),
                        getApplicationContext()));
        genres.setText(getString(R.string.show_genres) + " "
                + SeriesGuideData.splitAndKitTVDBStrings(show.getGenres()));
        String ratingText = show.getRating();
        if (ratingText != null && ratingText.length() != 0) {
            RatingBar ratingBar = (RatingBar) findViewById(R.id.bar);
            ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
            rating.setText(ratingText + "/10");
        }
        runtime.setText(getString(R.string.show_runtime) + " " + show.getRuntime() + " "
                + getString(R.string.show_airtimeunit));

        // IMDB button
        showInIMDB.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // track event
                AnalyticsUtils.getInstance(ShowInfo.this).trackEvent("ShowInfo", "Click",
                        "Show in IMDB", 0);

                String imdbid = show.getImdbId();
                if (imdbid.length() != 0) {

                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///title/"
                            + imdbid + "/"));
                    try {
                        startActivity(myIntent);
                    } catch (ActivityNotFoundException e) {
                        myIntent = new Intent(Intent.ACTION_VIEW, Uri
                                .parse(IMDB_TITLE_URL + imdbid));
                        startActivity(myIntent);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.noIMDBentry),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Poster
        Bitmap bitmap = imageCache.get(show.getPoster());
        if (bitmap != null) {
            showart.setImageBitmap(bitmap);
        } else {
            showart.setImageBitmap(null);
        }
    }

}
