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
 */

package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.ShowInfo;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.SimpleMenu;
import com.battlelancer.thetvdbapi.ImageCache;
import com.battlelancer.thetvdbapi.Series;
import com.battlelancer.thetvdbapi.TheTVDB;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverviewFragment extends Fragment {

    private boolean mDualPane;

    private Series show;

    private ImageCache imageCache;

    protected long episodeid;

    private FetchArtTask artTask;

    private String airdate;

    final private Bundle mShareData = new Bundle();

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent("Overview", "Click", label, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.overview_fragment, container, false);
        initializeViews(v);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Overview");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getShowId() == null || getShowId().length() == 0) {
            getActivity().finish();
        }

        imageCache = ((SeriesGuideApplication) getActivity().getApplication()).getImageCache();

        fillShowData();

        // populate the compatibility actionbar
        if (android.os.Build.VERSION.SDK_INT < 11) {
            SimpleMenu simpleMenu = new SimpleMenu(getActivity());
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.episodedetails_actions, simpleMenu);
            for (int i = 0; i < simpleMenu.size(); i++) {
                MenuItem item = simpleMenu.getItem(i);
                ((BaseActivity) getActivity()).getActivityHelper()
                        .addActionButtonCompatFromMenuItem(item);
            }
        }

        setHasOptionsMenu(true);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View seasonsFragment = getActivity().findViewById(R.id.fragment_seasons);
        mDualPane = seasonsFragment != null && seasonsFragment.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            showSeasons();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        onLoadEpisode();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodedetails_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_togglemark:
                fireTrackerEvent("Toggle watched");

                onMarkWatched();
                break;
            case R.id.menu_shareepisode:
                fireTrackerEvent("Share episode");

                ShareUtils.showShareDialog(getFragmentManager(), mShareData);

                break;
            case R.id.menu_addevent:
                fireTrackerEvent("Add episode to calendar");

                ShareUtils.onAddCalendarEvent(getActivity(), show.getSeriesName(),
                        mShareData.getString(ShareItems.EPISODESTRING), airdate, show.getAirsTime(),
                        show.getRuntime());
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews(View fragmentView) {
        fragmentView.findViewById(R.id.showinfo).setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                onShowShowInfo();
            }
        });
        View v = fragmentView.findViewById(R.id.gotoseasons);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    showSeasons();
                }
            });
        }
    }

    private void showSeasons() {
        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                    R.id.fragment_seasons);
            if (seasons == null) {
                // Make new fragment to show this selection.
                seasons = SeasonsFragment.newInstance(getShowId());

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_seasons, seasons);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), SeasonsActivity.class);
            intent.putExtra(BaseColumns._ID, getShowId());
            startActivity(intent);
        }

    }

    private String getShowId() {
        return getArguments().getString(Shows._ID);
    }

    private void fillShowData() {
        show = SeriesDatabase.getShow(getActivity(), getShowId());

        // Save imdbId for GetGlue sharing
        mShareData.putString(ShareItems.IMDBID, show.getImdbId());

        // Show name
        TextView showname = (TextView) getActivity().findViewById(R.id.seriesname);
        showname.setText(show.getSeriesName());

        // Running state
        TextView status = (TextView) getActivity().findViewById(R.id.showStatus);
        if (show.getStatus().equalsIgnoreCase("Continuing")) {
            status.setTextColor(Color.GREEN);
            status.setText(getString(R.string.show_isalive));
        } else if (show.getStatus().equalsIgnoreCase("Ended")) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // Poster
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
            // using alpha seems not to work on eclair, so only set a
            // background on froyo+ then
            final ImageView background = (ImageView) getActivity().findViewById(R.id.background);
            Bitmap bg = imageCache.get(show.getPoster());
            if (bg != null) {
                BitmapDrawable drawable = new BitmapDrawable(getResources(), bg);
                drawable.setAlpha(50);
                background.setImageDrawable(drawable);
            }
        }

        ImageView poster = (ImageView) getActivity().findViewById(R.id.showposter);
        Bitmap bitmap = imageCache.getThumb(show.getPoster());
        if (bitmap != null) {
            poster.setImageBitmap(bitmap);
        } else {
            poster.setImageResource(R.drawable.show_generic);
        }

        // Airtime and Network
        String timeAndNetwork = "";
        if (show.getAirsDayOfWeek().length() != 0 && show.getAirsTime() != -1) {
            String[] values = SeriesGuideData.parseMillisecondsToTime(show.getAirsTime(),
                    show.getAirsDayOfWeek(), true, getActivity());
            timeAndNetwork += values[1] + " " + values[0];
        }
        if (show.getNetwork().length() != 0) {
            timeAndNetwork += " " + getString(R.string.show_network) + " " + show.getNetwork();
        }
        TextView showmeta = (TextView) getActivity().findViewById(R.id.showmeta);
        showmeta.setText(timeAndNetwork);
    }

    private void fillEpisodeData() {
        TextView nextheader = (TextView) getActivity().findViewById(R.id.nextheader);
        TextView episodetitle = (TextView) getActivity().findViewById(R.id.TextViewEpisodeTitle);
        TextView numbers = (TextView) getActivity().findViewById(R.id.TextViewEpisodeNumbers);

        // // start share string
        String sharestring = getString(R.string.share_checkout);
        String episodestring = "";
        sharestring += " \"" + ((TextView) getActivity().findViewById(R.id.seriesname)).getText();

        if (episodeid != 0) {
            episodetitle.setVisibility(View.VISIBLE);
            numbers.setVisibility(View.VISIBLE);
            LinearLayout episodemeta = (LinearLayout) getActivity().findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.VISIBLE);

            // final Bundle episode = mDbHelper.getEpisodeDetails(episodeid);
            final Cursor episode = getActivity().getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(episodeid)), EpisodeQuery.PROJECTION,
                    null, null, null);
            episode.moveToFirst();

            // Airdate
            airdate = episode.getString(EpisodeQuery.FIRSTAIRED);
            if (airdate.length() != 0) {
                nextheader.setText(SeriesGuideData.parseDateToLocalRelative(airdate,
                        show.getAirsTime(), getActivity())
                        + ":");
            }

            onLoadEpisodeDetails(episode);

            // create share string
            episodestring = ShareUtils.onCreateShareString(getActivity(), episode);

            // Episode image
            String imagePath = episode.getString(EpisodeQuery.IMAGE);
            onLoadImage(imagePath);

            episode.close();
        } else {
            // no next episode, display single line info text, remove other
            // views
            nextheader.setText("  " + getString(R.string.no_nextepisode));
            episodetitle.setVisibility(View.GONE);
            numbers.setVisibility(View.GONE);
            LinearLayout episodemeta = (LinearLayout) getActivity().findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.GONE);
        }

        // finish share string
        sharestring += " - " + episodestring + "\" via @SeriesGuide";
        mShareData.putString(ShareItems.SHARESTRING, sharestring);
        mShareData.putString(ShareItems.EPISODESTRING, episodestring);
    }

    protected void onLoadEpisode() {
        new Thread(new Runnable() {
            public void run() {
                episodeid = SeriesDatabase.updateLatestEpisode(getActivity(), getShowId());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            fillEpisodeData();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Load title, numbers, description, dvdnumber, directors, guesstars,
     * writers and rating.
     * 
     * @param episode
     */
    protected void onLoadEpisodeDetails(final Cursor episode) {

        // Episode title
        TextView episodetitle = (TextView) getActivity().findViewById(R.id.TextViewEpisodeTitle);
        episodetitle.setText(episode.getString(EpisodeQuery.TITLE));

        // Season and Number
        TextView numbers = (TextView) getActivity().findViewById(R.id.TextViewEpisodeNumbers);

        // trakt
        mShareData.putInt(ShareItems.TVDBID, episode.getInt(EpisodeQuery.REF_SHOW_ID));
        mShareData.putInt(ShareItems.SEASON, episode.getInt(EpisodeQuery.SEASON));
        mShareData.putInt(ShareItems.EPISODE, episode.getInt(EpisodeQuery.NUMBER));

        numbers.setText(getString(R.string.season) + " " + episode.getString(EpisodeQuery.SEASON)
                + " " + getString(R.string.episode) + " " + episode.getString(EpisodeQuery.NUMBER));

        // Description
        TextView description = (TextView) getActivity().findViewById(
                R.id.TextViewEpisodeDescription);
        description.setText(episode.getString(EpisodeQuery.OVERVIEW));

        // DVD episode number
        TextView dvdnumber = (TextView) getActivity().findViewById(R.id.textViewEpisodeDVDnumber);
        dvdnumber.setText(getString(R.string.episode_dvdnumber) + ": "
                + episode.getString(EpisodeQuery.DVDNUMBER));

        // Directors
        TextView directors = (TextView) getActivity().findViewById(R.id.TextViewEpisodeDirectors);
        String directorsAll = SeriesGuideData.splitAndKitTVDBStrings(episode
                .getString(EpisodeQuery.DIRECTORS));
        directors.setText(getString(R.string.episode_directors) + " " + directorsAll);

        // Guest stars
        TextView gueststars = (TextView) getActivity().findViewById(R.id.TextViewEpisodeGuestStars);
        gueststars
                .setText(getString(R.string.episode_gueststars)
                        + " "
                        + SeriesGuideData.splitAndKitTVDBStrings(episode
                                .getString(EpisodeQuery.GUESTSTARS)));

        // Writers
        TextView writers = (TextView) getActivity().findViewById(R.id.TextViewEpisodeWriters);
        writers.setText(getString(R.string.episode_writers) + " "
                + SeriesGuideData.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.WRITERS)));

        // Rating
        TextView rating = (TextView) getActivity().findViewById(R.id.value);
        String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            RatingBar ratingBar = (RatingBar) getActivity().findViewById(R.id.bar);
            ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
            rating.setText(ratingText + "/10");
        }
    }

    private void onMarkWatched() {
        SeriesDatabase.markEpisode(getActivity(), String.valueOf(episodeid), true);

        Toast.makeText(getActivity(), getString(R.string.mark_episode), Toast.LENGTH_SHORT).show();

        // share with trakt
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity()
                .getApplicationContext());
        if (prefs.getBoolean("com.battlelancer.seriesguide.traktintegration", false)) {
            new ShareUtils.TraktTask(getActivity(), getFragmentManager(), mShareData).execute();
        }

        // load new episode, update seasons (if shown)
        onLoadEpisode();
        SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_seasons);
        if (seasons != null) {
            seasons.updateUnwatchedCounts(false);
        }
    }

    protected void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getActivity().findViewById(R.id.imageContainer);

        if (imagePath.length() != 0) {
            final ImageView imageView = (ImageView) container
                    .findViewById(R.id.ImageViewEpisodeImage);
            final Bitmap bitmap = imageCache.get(imagePath);
            if (bitmap != null) {
                // image is in cache
                imageView.setImageBitmap(bitmap);
            } else {
                if (artTask == null) {
                    artTask = (FetchArtTask) new FetchArtTask(imagePath, imageView).execute();
                } else if (artTask != null && artTask.getStatus() == AsyncTask.Status.FINISHED) {
                    artTask = (FetchArtTask) new FetchArtTask(imagePath, imageView).execute();
                }
            }
        } else {
            // no image available
            container.setVisibility(View.GONE);
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Integer> {
        private static final int SUCCESS = 0;

        private static final int ERROR = 1;

        private String mPath;

        private ImageView mImageView;

        protected FetchArtTask(String path, ImageView imageView) {
            mPath = path;
            mImageView = imageView;
        }

        @Override
        protected void onPreExecute() {
            mImageView.setImageResource(R.drawable.ic_menu_refresh);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int resultCode;

            if (TheTVDB.fetchArt(mPath, false, getActivity())) {
                resultCode = SUCCESS;
            } else {
                resultCode = ERROR;
            }

            return resultCode;
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            if (isCancelled()) {
                return;
            }

            switch (resultCode) {
                case SUCCESS:
                    Bitmap bitmap = imageCache.get(mPath);
                    if (bitmap != null) {
                        mImageView.setImageBitmap(bitmap);
                        return;
                    }
                    // no break because image could be null (got deleted, ...)
                default:
                    // fallback
                    mImageView.setImageResource(R.drawable.show_generic);
                    break;
            }
        }
    }

    /**
     * Launch show info activity.
     */
    private void onShowShowInfo() {
        Intent i = new Intent(getActivity(), ShowInfo.class);
        i.putExtra(Shows._ID, getShowId());
        startActivity(i);
    }

    interface EpisodeQuery {
        String[] PROJECTION = new String[] {
                Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW, Episodes.NUMBER,
                Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIRED, Episodes.DIRECTORS,
                Episodes.GUESTSTARS, Episodes.WRITERS, Episodes.RATING, Episodes.IMAGE,
                Episodes.DVDNUMBER, Episodes.TITLE
        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRSTAIRED = 6;

        int DIRECTORS = 7;

        int GUESTSTARS = 8;

        int WRITERS = 9;

        int RATING = 10;

        int IMAGE = 11;

        int DVDNUMBER = 12;

        int TITLE = 13;
    }
}
