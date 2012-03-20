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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.ShareUtils.TraktTask.OnTraktActionCompleteListener;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.ImageCache;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverviewFragment extends SherlockFragment implements OnTraktActionCompleteListener {

    private boolean mDualPane;

    private Series mShow;

    private ImageCache imageCache;

    protected long mEpisodeid;

    private FetchArtTask artTask;

    final private Bundle mShareData = new Bundle();

    private long mAirtime;

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

        imageCache = ImageCache.getInstance(getActivity());

        fillShowData();

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
        inflater.inflate(R.menu.overview_menu, menu);

        // use an appropriate quick share button
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getSherlockActivity());
        int lastShareAction = prefs.getInt(SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, -1);

        MenuItem shareAction = menu.findItem(R.id.menu_quickshare);
        if (lastShareAction != -1) {
            ShareMethod shareMethod = ShareMethod.values()[lastShareAction];
            shareAction.setTitle(shareMethod.titleRes);
            shareAction.setIcon(shareMethod.drawableRes);
        } else {
            shareAction.setEnabled(false);
            shareAction.setVisible(false);
        }

        if (mEpisodeid == 0) {
            menu.findItem(R.id.menu_addevent).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_togglemark:
                fireTrackerEvent("Toggle watched");

                onMarkWatched();
                break;
            case R.id.menu_quickshare: {
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                int shareMethodIndex = prefs.getInt(
                        SeriesGuidePreferences.KEY_LAST_USED_SHARE_METHOD, -1);
                ShareMethod shareMethod = ShareMethod.values()[shareMethodIndex];

                fireTrackerEvent("Quick share (" + shareMethod.name() + ")");

                onShareEpisode(shareMethod, false);
                break;
            }
            case R.id.menu_checkin_getglue: {
                fireTrackerEvent("Check In (GetGlue)");
                onShareEpisode(ShareMethod.CHECKIN_GETGLUE, true);
                break;
            }
            case R.id.menu_checkin_trakt: {
                fireTrackerEvent("Check In (trakt)");
                onShareEpisode(ShareMethod.CHECKIN_TRAKT, true);
                break;
            }
            case R.id.menu_markseen_trakt: {
                fireTrackerEvent("Mark seen (trakt)");
                onShareEpisode(ShareMethod.MARKSEEN_TRAKT, true);
                break;
            }
            case R.id.menu_rate_trakt: {
                fireTrackerEvent("Rate (trakt)");
                onShareEpisode(ShareMethod.RATE_TRAKT, true);
                break;
            }
            case R.id.menu_share_others: {
                fireTrackerEvent("Share (apps)");
                onShareEpisode(ShareMethod.OTHER_SERVICES, true);
                break;
            }
            case R.id.menu_addevent: {
                fireTrackerEvent("Add episode to calendar");

                ShareUtils.onAddCalendarEvent(getActivity(), mShow.getSeriesName(),
                        mShareData.getString(ShareItems.EPISODESTRING), mAirtime,
                        mShow.getRuntime());
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShareEpisode(ShareMethod shareMethod, boolean isInvalidateOptionsMenu) {
        ShareUtils.onShareEpisode(getActivity(), mShareData, shareMethod, this);

        if (isInvalidateOptionsMenu) {
            // invalidate the options menu so a potentially new
            // quick share action is displayed
            getSherlockActivity().invalidateOptionsMenu();
        }
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
        mShow = DBUtils.getShow(getActivity(), getShowId());

        if (mShow == null) {
            return;
        }

        // Save info for sharing
        mShareData.putString(ShareItems.IMDBID, mShow.getImdbId());
        mShareData.putInt(ShareItems.TVDBID, Integer.valueOf(mShow.getId()));

        // Show name
        TextView showname = (TextView) getActivity().findViewById(R.id.seriesname);
        showname.setText(mShow.getSeriesName());

        // Running state
        TextView status = (TextView) getActivity().findViewById(R.id.showStatus);
        if (mShow.getStatus() == 1) {
            status.setTextColor(Color.GREEN);
            status.setText(getString(R.string.show_isalive));
        } else if (mShow.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // Poster
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
            // using alpha seems not to work on eclair, so only set a
            // background on froyo+ then
            final ImageView background = (ImageView) getActivity().findViewById(R.id.background);
            Bitmap bg = imageCache.get(mShow.getPoster());
            if (bg != null) {
                BitmapDrawable drawable = new BitmapDrawable(getResources(), bg);
                drawable.setAlpha(50);
                background.setImageDrawable(drawable);
            }
        }

        // Airtime and Network
        String timeAndNetwork = "";
        if (mShow.getAirsDayOfWeek().length() != 0 && mShow.getAirsTime() != -1) {
            String[] values = Utils.parseMillisecondsToTime(mShow.getAirsTime(),
                    mShow.getAirsDayOfWeek(), getActivity());
            timeAndNetwork += values[1] + " " + values[0];
        } else {
            timeAndNetwork += getString(R.string.show_noairtime);
        }
        if (mShow.getNetwork().length() != 0) {
            timeAndNetwork += " " + getString(R.string.show_network) + " " + mShow.getNetwork();
        }
        TextView showmeta = (TextView) getActivity().findViewById(R.id.showmeta);
        showmeta.setText(timeAndNetwork);
    }

    private void fillEpisodeData() {
        final Activity context = getActivity();
        if (context == null) {
            return;
        }

        TextView nextheader = (TextView) context.findViewById(R.id.nextheader);
        TextView episodetitle = (TextView) context.findViewById(R.id.TextViewEpisodeTitle);
        TextView numbers = (TextView) context.findViewById(R.id.TextViewEpisodeNumbers);

        // // start share string
        String sharestring = getString(R.string.share_checkout);
        String episodestring = "";
        sharestring += " \"" + ((TextView) context.findViewById(R.id.seriesname)).getText();

        if (mEpisodeid != 0) {
            episodetitle.setVisibility(View.VISIBLE);
            numbers.setVisibility(View.VISIBLE);
            LinearLayout episodemeta = (LinearLayout) context.findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.VISIBLE);

            // final Bundle episode = mDbHelper.getEpisodeDetails(episodeid);
            final Cursor episode = context.getContentResolver().query(
                    Episodes.buildEpisodeUri(String.valueOf(mEpisodeid)), EpisodeQuery.PROJECTION,
                    null, null, null);
            episode.moveToFirst();

            // Airdate
            mAirtime = episode.getLong(EpisodeQuery.FIRSTAIREDMS);
            if (mAirtime != -1) {
                final String[] dayAndTime = Utils.formatToTimeAndDay(mAirtime, context);
                nextheader.setText(dayAndTime[2] + " (" + dayAndTime[1] + "):");
            }

            onLoadEpisodeDetails(episode);

            // create share string
            episodestring = ShareUtils.onCreateShareString(context, episode);

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
            LinearLayout episodemeta = (LinearLayout) context.findViewById(R.id.episodemeta);
            episodemeta.setVisibility(View.GONE);
        }

        // finish share string
        sharestring += " - " + episodestring + "\" via @SeriesGuide";
        mShareData.putString(ShareItems.SHARESTRING, sharestring);
        mShareData.putString(ShareItems.EPISODESTRING, episodestring);
    }

    protected void onLoadEpisode() {
        new AsyncTask<Void, Void, Integer>() {

            private SherlockFragmentActivity activity;

            @Override
            protected Integer doInBackground(Void... params) {
                activity = getSherlockActivity();
                if (activity == null) {
                    return -1;
                }
                mEpisodeid = DBUtils.updateLatestEpisode(activity, getShowId());
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == 0 && isAdded()) {
                    activity.invalidateOptionsMenu();
                    fillEpisodeData();
                }
            }

        }.execute();
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
        final String title = episode.getString(EpisodeQuery.TITLE);
        episodetitle.setText(title);

        // Season and Number
        TextView numbers = (TextView) getActivity().findViewById(R.id.TextViewEpisodeNumbers);

        // trakt
        final int showTvdbId = episode.getInt(EpisodeQuery.REF_SHOW_ID);
        final int season = episode.getInt(EpisodeQuery.SEASON);
        final int number = episode.getInt(EpisodeQuery.NUMBER);
        mShareData.putInt(ShareItems.TVDBID, showTvdbId);
        mShareData.putInt(ShareItems.SEASON, season);
        mShareData.putInt(ShareItems.EPISODE, number);

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
        String directorsAll = Utils.splitAndKitTVDBStrings(episode
                .getString(EpisodeQuery.DIRECTORS));
        directors.setText(getString(R.string.episode_directors) + " " + directorsAll);

        // Guest stars
        TextView gueststars = (TextView) getActivity().findViewById(R.id.TextViewEpisodeGuestStars);
        gueststars.setText(getString(R.string.episode_gueststars) + " "
                + Utils.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));

        // Writers
        TextView writers = (TextView) getActivity().findViewById(R.id.TextViewEpisodeWriters);
        writers.setText(getString(R.string.episode_writers) + " "
                + Utils.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.WRITERS)));

        // TVDb rating
        TextView rating = (TextView) getActivity().findViewById(R.id.value);
        String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            RatingBar ratingBar = (RatingBar) getActivity().findViewById(R.id.bar);
            ratingBar.setProgress((int) (Double.valueOf(ratingText) / 0.1));
            rating.setText(ratingText + "/10");
        }

        // trakt rating
        new TraktSummaryTask(getSherlockActivity(), getView()).episode(showTvdbId, season, number)
                .execute();

        // TVDb button
        getView().findViewById(R.id.buttonShowInfoIMDB).setVisibility(View.GONE);
        final String seasonId = episode.getString(EpisodeQuery.REF_SEASON_ID);
        final String episodeId = episode.getString(EpisodeQuery._ID);
        getView().findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TVDB_EPISODE_URL_1
                        + showTvdbId + Constants.TVDB_EPISODE_URL_2 + seasonId
                        + Constants.TVDB_EPISODE_URL_3 + episodeId));
                startActivity(i);
            }
        });

        // trakt shouts button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDualPane) {
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), TraktShoutsActivity.class);
                    intent.putExtra(ShareItems.TVDBID, showTvdbId);
                    intent.putExtra(ShareItems.SEASON, season);
                    intent.putExtra(ShareItems.EPISODE, number);
                    intent.putExtra(ShareItems.SHARESTRING, title);
                    startActivity(intent);
                } else {
                    TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(title,
                            showTvdbId, season, number);
                    newFragment.show(getFragmentManager(), "shouts-dialog");
                }
            }
        });
    }

    private void onMarkWatched() {
        DBUtils.markEpisode(getActivity(), String.valueOf(mEpisodeid), true);

        Toast.makeText(getActivity(), getString(R.string.mark_episode), Toast.LENGTH_SHORT).show();

        // load new episode, update seasons (if shown)
        onLoadEpisode();
        onUpdateSeasons();
    }

    private void onUpdateSeasons() {
        SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_seasons);
        if (seasons != null) {
            seasons.updateUnwatchedCounts(false);
        }
    }

    protected void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getActivity().findViewById(R.id.imageContainer);

        if (imagePath.length() != 0) {
            container.setVisibility(View.VISIBLE);
            final ImageView imageView = (ImageView) container
                    .findViewById(R.id.ImageViewEpisodeImage);
            final Bitmap bitmap = imageCache.get(imagePath);
            if (bitmap != null) {
                // image is in cache
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageBitmap(bitmap);
            } else {
                if (artTask == null) {
                    artTask = (FetchArtTask) new FetchArtTask(imagePath, imageView, getActivity())
                            .execute();
                } else if (artTask != null && artTask.getStatus() == AsyncTask.Status.FINISHED) {
                    artTask = (FetchArtTask) new FetchArtTask(imagePath, imageView, getActivity())
                            .execute();
                }
            }
        } else {
            // no image available
            container.setVisibility(View.GONE);
        }
    }

    /**
     * Launch show info activity.
     */
    private void onShowShowInfo() {
        Intent i = new Intent(getActivity(), ShowInfoActivity.class);
        i.putExtra(Shows._ID, getShowId());
        startActivity(i);
    }

    @Override
    public void onTraktActionComplete(boolean wasSuccessfull) {
        // load new episode, update seasons (if shown)
        onLoadEpisode();
        onUpdateSeasons();
    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[] {
                Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW, Episodes.NUMBER,
                Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS, Episodes.DIRECTORS,
                Episodes.GUESTSTARS, Episodes.WRITERS, Episodes.RATING, Episodes.IMAGE,
                Episodes.DVDNUMBER, Episodes.TITLE, Seasons.REF_SEASON_ID
        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRSTAIREDMS = 6;

        int DIRECTORS = 7;

        int GUESTSTARS = 8;

        int WRITERS = 9;

        int RATING = 10;

        int IMAGE = 11;

        int DVDNUMBER = 12;

        int TITLE = 13;

        int REF_SEASON_ID = 14;
    }
}
