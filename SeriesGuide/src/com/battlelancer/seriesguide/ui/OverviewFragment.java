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

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagAction;
import com.battlelancer.seriesguide.util.FlagTask.OnFlagListener;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays general information about a show and its next episode. Displays a
 * {@link SeasonsFragment} on larger screens.
 */
public class OverviewFragment extends SherlockFragment implements OnTraktActionCompleteListener,
        OnFlagListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "OverviewFragment";

    private static final int EPISODE_LOADER_ID = 100;

    private static final int SHOW_LOADER_ID = 101;

    private boolean mDualPane;

    private FetchArtTask mArtTask;

    private TraktSummaryTask mTraktTask;

    private View mSeasonsButton;

    private Cursor mEpisodeCursor;

    private Cursor mShowCursor;

    private String mShowTitle;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {
        String SHOW_TVDBID = "show_tvdbid";
    }

    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent("Overview", "Click", label, (long) 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.overview_fragment, container, false);
        v.findViewById(R.id.showinfo).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                onShowShowInfo(v);
            }
        });
        mSeasonsButton = v.findViewById(R.id.gotoseasons);
        if (mSeasonsButton != null) {
            mSeasonsButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    onShowSeasons();
                }
            });
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getShowId() == 0) {
            getActivity().finish();
            return;
        }

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View seasonsFragment = getActivity().findViewById(R.id.fragment_seasons);
        mDualPane = seasonsFragment != null && seasonsFragment.getVisibility() == View.VISIBLE;
        if (mDualPane) {
            onShowSeasons();
        }

        getLoaderManager().initLoader(SHOW_LOADER_ID, null, this);
        getLoaderManager().initLoader(EPISODE_LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        if (mTraktTask != null) {
            mTraktTask.cancel(true);
            mTraktTask = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.overview_menu, menu);

        // enable/disable menu items
        boolean isEpisodeVisible;
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            isEpisodeVisible = true;
            boolean isCollected = mEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1 ? true : false;
            menu.findItem(R.id.menu_flag_collected).setIcon(
                    isCollected ? R.drawable.ic_collected : R.drawable.ic_action_collect);
        } else {
            isEpisodeVisible = false;
        }
        menu.findItem(R.id.menu_checkin).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_flag_watched).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_flag_collected).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_calendarevent).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_share).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_rate_trakt).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_manage_lists).setEnabled(isEpisodeVisible);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_checkin) {
            if (mShowCursor != null && mShowCursor.moveToFirst() && mEpisodeCursor != null
                    && mEpisodeCursor.moveToFirst()) {
                final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
                final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
                // check in
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(
                        mShowCursor.getString(ShowQuery.SHOW_IMDBID),
                        getShowId(),
                        seasonNumber,
                        episodeNumber,
                        buildEpisodeString(seasonNumber, episodeNumber,
                                mEpisodeCursor.getString(EpisodeQuery.TITLE)));
                f.show(getFragmentManager(), "checkin-dialog");

            }
            fireTrackerEvent("Check In");
            return true;
        } else if (itemId == R.id.menu_flag_watched) {
            // flag watched
            onFlagWatched();
            fireTrackerEvent("Flag Watched");
            return true;
        } else if (itemId == R.id.menu_flag_collected) {
            // toggle collected
            onToggleCollected(item);
            fireTrackerEvent("Toggle Collected");
            return true;
        } else if (itemId == R.id.menu_calendarevent) {
            if (mShowCursor != null && mShowCursor.moveToFirst() && mEpisodeCursor != null
                    && mEpisodeCursor.moveToFirst()) {
                final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
                final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
                final String episodeTitle = mEpisodeCursor.getString(EpisodeQuery.TITLE);
                // add calendar event
                ShareUtils.onAddCalendarEvent(
                        getActivity(),
                        mShowCursor.getString(ShowQuery.SHOW_TITLE),
                        buildEpisodeString(seasonNumber, episodeNumber,
                                episodeTitle), mEpisodeCursor.getLong(EpisodeQuery.FIRSTAIREDMS),
                        mShowCursor.getInt(ShowQuery.SHOW_RUNTIME));
            }
            fireTrackerEvent("Add to calendar");
            return true;
        } else if (itemId == R.id.menu_rate_trakt) {
            // rate episode on trakt.tv
            onShareEpisode(ShareMethod.RATE_TRAKT);
            fireTrackerEvent("Rate (trakt)");
            return true;
        } else if (itemId == R.id.menu_share) {
            // share episode
            onShareEpisode(ShareMethod.OTHER_SERVICES);
            fireTrackerEvent("Share (apps)");
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
                ListsDialogFragment.showListsDialog(mEpisodeCursor.getString(EpisodeQuery._ID),
                        3, getFragmentManager());
            }
            return true;
        } else if (itemId == R.id.menu_search) {
            // search through this shows episodes
            getActivity().onSearchRequested();
            fireTrackerEvent("Search show episodes");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onShowSeasons() {
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
                ft.setCustomAnimations(R.anim.fragment_slide_left_enter,
                        R.anim.fragment_slide_left_exit);
                ft.replace(R.id.fragment_seasons, seasons);
                ft.commit();
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), SeasonsActivity.class);
            intent.putExtra(SeasonsFragment.InitBundle.SHOW_TVDBID, getShowId());
            startActivity(intent);
            getSherlockActivity().overridePendingTransition(R.anim.fragment_slide_left_enter,
                    R.anim.fragment_slide_left_exit);
        }

    }

    /**
     * Launch show info activity.
     */
    @TargetApi(16)
    private void onShowShowInfo(View sourceView) {
        Intent i = new Intent(getActivity(), ShowInfoActivity.class);
        i.putExtra(ShowInfoActivity.InitBundle.SHOW_TVDBID, getShowId());

        if (AndroidUtils.isJellyBeanOrHigher()) {
            Bundle options = ActivityOptions.makeScaleUpAnimation(sourceView, 0, 0,
                    sourceView.getWidth(),
                    sourceView.getHeight()).toBundle();
            getActivity().startActivity(i, options);
        } else {
            startActivity(i);
        }
    }

    private void onShareEpisode(ShareMethod shareMethod) {
        if (mShowCursor != null && mShowCursor.moveToFirst() && mEpisodeCursor != null
                && mEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);

            // build share data
            Bundle shareData = new Bundle();
            shareData.putInt(ShareItems.SEASON, seasonNumber);
            shareData.putInt(ShareItems.EPISODE, episodeNumber);
            shareData.putInt(ShareItems.TVDBID, getShowId());

            String episodestring = buildEpisodeString(seasonNumber, episodeNumber,
                    mEpisodeCursor.getString(EpisodeQuery.TITLE));
            shareData.putString(ShareItems.EPISODESTRING, episodestring);

            final StringBuilder shareString = new
                    StringBuilder(getString(R.string.share_checkout));
            shareString.append(" \"").append(mShowCursor.getString(ShowQuery.SHOW_TITLE));
            shareString.append(" - ").append(episodestring).append("\"");
            shareData.putString(ShareItems.SHARESTRING, shareString.toString());

            String imdbId = mEpisodeCursor.getString(EpisodeQuery.IMDBID);
            if (TextUtils.isEmpty(imdbId)) {
                // fall back to show IMDb id
                imdbId = mShowCursor.getString(ShowQuery.SHOW_IMDBID);
            }
            shareData.putString(ShareItems.IMDBID, imdbId);

            ShareUtils.onShareEpisode(getActivity(), shareData, shareMethod, this);
        }
    }

    private String buildEpisodeString(int seasonNumber, int episodeNumber, String episodeTitle) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        return Utils.getNextEpisodeString(prefs, seasonNumber,
                episodeNumber, episodeTitle);
    }

    private void onFlagWatched() {
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            new FlagTask(getActivity(), getShowId(), this)
                    .episodeWatched(seasonNumber, episodeNumber)
                    .setItemId(mEpisodeCursor.getInt(EpisodeQuery._ID)).setFlag(true)
                    .execute();
        }
    }

    private void onToggleCollected(MenuItem item) {
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final boolean isCollected = mEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1 ? true
                    : false;
            new FlagTask(getActivity(), getShowId(), null)
                    .episodeCollected(seasonNumber, episodeNumber)
                    .setItemId(mEpisodeCursor.getInt(EpisodeQuery._ID))
                    .setFlag(!isCollected).execute();

            item.setIcon(isCollected ? R.drawable.ic_collected : R.drawable.ic_action_collect);
        }
    }

    private void onUpdateSeasons() {
        SeasonsFragment seasons = (SeasonsFragment) getFragmentManager().findFragmentById(
                R.id.fragment_seasons);
        if (seasons != null) {
            seasons.updateUnwatchedCounts();
        }
    }

    @Override
    public void onTraktActionComplete(boolean wasSuccessfull) {
        if (isAdded()) {
            // load new episode, update seasons (if shown)
            // TODO onLoadEpisode();
            onUpdateSeasons();
        }
    }

    @Override
    public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful) {
        if (isSuccessful && isAdded()) {
            // load new episode, update seasons (if shown)
            // TODO onLoadEpisode();
            onUpdateSeasons();
        }
    }

    public static class EpisodeLoader extends CursorLoader {

        private String mShowId;

        public EpisodeLoader(Context context, String showId) {
            super(context);
            mShowId = showId;
            setProjection(EpisodeQuery.PROJECTION);
        }

        @Override
        public Cursor loadInBackground() {
            // get episode id, set query params
            int episodeId = (int) DBUtils.updateLatestEpisode(getContext(), mShowId);
            setUri(Episodes.buildEpisodeWithShowUri(String.valueOf(episodeId)));

            return super.loadInBackground();
        }

    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW,
                Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS,
                Episodes.GUESTSTARS, Tables.EPISODES + "." + Episodes.RATING,
                Episodes.IMAGE, Episodes.DVDNUMBER, Episodes.TITLE, Seasons.REF_SEASON_ID,
                Episodes.COLLECTED, Episodes.IMDBID, Episodes.ABSOLUTE_NUMBER

        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRSTAIREDMS = 6;

        int GUESTSTARS = 7;

        int RATING = 8;

        int IMAGE = 9;

        int DVDNUMBER = 10;

        int TITLE = 11;

        int REF_SEASON_ID = 12;

        int COLLECTED = 13;

        int IMDBID = 14;

        int ABSOLUTE_NUMBER = 15;

    }

    interface ShowQuery {

        String[] PROJECTION = new String[] {
                Shows._ID, Shows.TITLE, Shows.STATUS, Shows.AIRSTIME, Shows.AIRSDAYOFWEEK,
                Shows.NETWORK, Shows.POSTER, Shows.IMDBID, Shows.RUNTIME
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_AIRSTIME = 3;
        int SHOW_AIRSDAYOFWEEK = 4;
        int SHOW_NETWORK = 5;
        int SHOW_POSTER = 6;
        int SHOW_IMDBID = 7;
        int SHOW_RUNTIME = 8;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case EPISODE_LOADER_ID:
            default:
                return new EpisodeLoader(getActivity(), String.valueOf(getShowId()));
            case SHOW_LOADER_ID:
                return new CursorLoader(getActivity(), Shows.buildShowUri(String
                        .valueOf(getShowId())), ShowQuery.PROJECTION, null, null, null);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (isAdded()) {
            switch (loader.getId()) {
                case EPISODE_LOADER_ID:
                    getSherlockActivity().invalidateOptionsMenu();
                    onPopulateEpisodeData(data);
                    onLoadRemainingCounter();
                    break;
                case SHOW_LOADER_ID:
                    onPopulateShowData(data);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case EPISODE_LOADER_ID:
                mEpisodeCursor = null;
                break;
            case SHOW_LOADER_ID:
                mShowCursor = null;
                break;
        }
    }

    private void onPopulateEpisodeData(Cursor episode) {
        mEpisodeCursor = episode;

        final TextView episodeTitle = (TextView) getView().findViewById(R.id.episodeTitle);
        final TextView episodeTime = (TextView) getView().findViewById(R.id.episodeTime);
        final TextView episodeInfo = (TextView) getView().findViewById(R.id.episodeInfo);
        final View episodemeta = getView().findViewById(R.id.episode_meta_container);
        final View episodePrimaryContainer = getView().findViewById(R.id.episode_primary_container);
        final View episodePrimaryClicker = getView().findViewById(R.id.episode_primary_click_dummy);

        if (episode != null && episode.moveToFirst()) {
            episodePrimaryContainer.setBackgroundResource(0);

            // some episode properties
            final int episodeId = episode.getInt(EpisodeQuery._ID);
            final int seasonNumber = episode.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = episode.getInt(EpisodeQuery.NUMBER);
            final int episodeAbsoluteNumber = episode.getInt(EpisodeQuery.ABSOLUTE_NUMBER);
            final String title = episode.getString(EpisodeQuery.TITLE);

            // title
            episodeTitle.setText(title);
            episodeTitle.setVisibility(View.VISIBLE);

            // number
            StringBuilder infoText = new StringBuilder();
            infoText.append(getString(R.string.season)).append(" ").append(seasonNumber);
            infoText.append(" ");
            infoText.append(getString(R.string.episode)).append(" ")
                    .append(episodeNumber);
            if (episodeAbsoluteNumber > 0) {
                infoText.append(" (").append(episodeAbsoluteNumber).append(")");
            }
            episodeInfo.setText(infoText);
            episodeInfo.setVisibility(View.VISIBLE);

            // air date
            long airtime = episode.getLong(EpisodeQuery.FIRSTAIREDMS);
            if (airtime != -1) {
                final String[] dayAndTime = Utils.formatToTimeAndDay(airtime, getActivity());
                episodeTime.setText(new StringBuilder().append(dayAndTime[2]).append(" (")
                        .append(dayAndTime[1])
                        .append(")"));
                episodeTime.setVisibility(View.VISIBLE);
            }

            // make title and image clickable
            episodePrimaryClicker.setOnClickListener(new OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onClick(View view) {
                    // display episode details
                    Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

                    if (AndroidUtils.isJellyBeanOrHigher()) {
                        Bundle options = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                                view.getWidth(), view.getHeight()).toBundle();
                        getActivity().startActivity(intent, options);
                    } else {
                        startActivity(intent);
                    }
                }
            });
            episodePrimaryClicker.setFocusable(true);

            // load all other info
            onLoadEpisodeDetails(episode);

            // episode image
            onLoadImage(episode.getString(EpisodeQuery.IMAGE));

            episodemeta.setVisibility(View.VISIBLE);
        } else {
            // no next episode: display single line info text, remove other
            // views
            episodeTitle.setText(R.string.no_nextepisode);
            episodeTime.setVisibility(View.GONE);
            episodeInfo.setVisibility(View.GONE);
            episodemeta.setVisibility(View.GONE);
            episodePrimaryContainer.setBackgroundResource(R.color.background_dim);
            episodePrimaryClicker.setOnClickListener(null);
            episodePrimaryClicker.setClickable(false);
            episodePrimaryClicker.setFocusable(false);
            onLoadImage(null);
        }

        // enable/disable applicable menu items
        getSherlockActivity().invalidateOptionsMenu();

        // animate view into visibility
        final View contentContainer = getView().findViewById(R.id.content_container);
        if (contentContainer.getVisibility() == View.GONE) {
            final View progressContainer = getView().findViewById(R.id.progress_container);
            progressContainer.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
        }

    }

    private void onLoadEpisodeDetails(final Cursor episode) {
        final int seasonNumber = episode.getInt(EpisodeQuery.SEASON);
        final int episodeNumber = episode.getInt(EpisodeQuery.NUMBER);
        final String episodeTitle = episode.getString(EpisodeQuery.TITLE);

        // Description, DVD episode number, guest stars, absolute number
        ((TextView) getView().findViewById(R.id.TextViewEpisodeDescription)).setText(episode
                .getString(EpisodeQuery.OVERVIEW));
        Utils.setLabelValueOrHide(getView().findViewById(R.id.labelDvd), (TextView) getView()
                .findViewById(R.id.textViewEpisodeDVDnumber), episode
                .getString(EpisodeQuery.DVDNUMBER));
        Utils.setLabelValueOrHide(getView().findViewById(R.id.labelGuestStars),
                (TextView) getView().findViewById(R.id.TextViewEpisodeGuestStars), Utils
                        .splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));

        // TVDb rating
        final String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            ((RatingBar) getView().findViewById(R.id.bar)).setProgress((int) (Double
                    .valueOf(ratingText) / 0.1));
            ((TextView) getView().findViewById(R.id.value)).setText(ratingText + "/10");
        }

        // Google Play button
        View playButton = getView().findViewById(R.id.buttonGooglePlay);
        Utils.setUpGooglePlayButton(mShowTitle + " " + episodeTitle, playButton, TAG);

        // Amazon button
        View amazonButton = getView().findViewById(R.id.buttonAmazon);
        Utils.setUpAmazonButton(mShowTitle + " " + episodeTitle, amazonButton, TAG);

        // IMDb button
        String imdbId = episode.getString(EpisodeQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId) && mShowCursor != null) {
            // fall back to show IMDb id
            imdbId = mShowCursor.getString(ShowQuery.SHOW_IMDBID);
        }
        Utils.setUpImdbButton(imdbId, getView().findViewById(R.id.buttonShowInfoIMDB), TAG,
                getActivity());

        // TVDb button
        final String seasonId = episode.getString(EpisodeQuery.REF_SEASON_ID);
        getView().findViewById(R.id.buttonTVDB).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri
                            .parse(Constants.TVDB_EPISODE_URL_1
                                    + getShowId() + Constants.TVDB_EPISODE_URL_2 + seasonId
                                    + Constants.TVDB_EPISODE_URL_3
                                    + mEpisodeCursor.getString(EpisodeQuery._ID)));
                    startActivity(i);
                }
            }
        });

        // trakt shouts button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {

                    if (!mDualPane) {
                        Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                        i.putExtras(TraktShoutsActivity.createInitBundle(getShowId(),
                                seasonNumber, episodeNumber, episodeTitle));
                        startActivity(i);
                    } else {
                        TraktShoutsFragment newFragment = TraktShoutsFragment.newInstance(
                                episodeTitle, getShowId(), seasonNumber, episodeNumber);
                        newFragment.show(getFragmentManager(), "shouts-dialog");
                    }
                }
            }
        });

        // trakt ratings
        mTraktTask = new TraktSummaryTask(getSherlockActivity(), getView()).episode(getShowId(),
                seasonNumber, episodeNumber);
        AndroidUtils.executeAsyncTask(mTraktTask, new Void[] {
                null
        });
    }

    private void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getView().findViewById(R.id.imageContainer);

        // clean up a previous task
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        mArtTask = (FetchArtTask) new FetchArtTask(imagePath, container, getActivity());
        AndroidUtils.executeAsyncTask(mArtTask, new Void[] {
                null
        });
    }

    private void onLoadRemainingCounter() {
        if (!mDualPane) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());

            AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {

                private TextView mRemainingView;

                @Override
                protected void onPreExecute() {
                    final View view = getView().findViewById(R.id.textViewRemaining);
                    if (view == null) {
                        cancel(true);
                    }

                    mRemainingView = (TextView) view;
                }

                @Override
                protected String doInBackground(String... params) {
                    if (isCancelled()) {
                        return null;
                    }
                    return DBUtils.getUnwatchedEpisodesOfShow(getActivity(),
                            params[0],
                            prefs);
                }

                @Override
                protected void onPostExecute(String result) {
                    mRemainingView.setText(result);
                }

            };
            AndroidUtils.executeAsyncTask(task, String.valueOf(getShowId()));
        }
    }

    private void onPopulateShowData(Cursor show) {
        if (show == null || !show.moveToFirst()) {
            return;
        }
        mShowCursor = show;

        // title
        mShowTitle = show.getString(ShowQuery.SHOW_TITLE);
        ((TextView) getView().findViewById(R.id.seriesname)).setText(mShowTitle);

        // status
        final TextView statusText = (TextView) getView().findViewById(R.id.showStatus);
        int status = show.getInt(ShowQuery.SHOW_STATUS);
        if (status == 1) {
            TypedValue outValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.textColorSgGreen,
                    outValue, true);
            statusText.setTextColor(getResources().getColor(outValue.resourceId));
            statusText.setText(getString(R.string.show_isalive));
        } else if (status == 0) {
            statusText.setTextColor(Color.GRAY);
            statusText.setText(getString(R.string.show_isnotalive));
        }

        // poster
        final ImageView background = (ImageView) getView().findViewById(R.id.background);
        Utils.setPosterBackground(background, show.getString(ShowQuery.SHOW_POSTER),
                getActivity());

        // air time and network
        final StringBuilder timeAndNetwork = new StringBuilder();
        final String airsDay = show.getString(ShowQuery.SHOW_AIRSDAYOFWEEK);
        final long airstime = show.getLong(ShowQuery.SHOW_AIRSTIME);
        if (!TextUtils.isEmpty(airsDay) && airstime != -1) {
            String[] values = Utils.parseMillisecondsToTime(airstime,
                    airsDay, getActivity());
            timeAndNetwork.append(values[1]).append(" ").append(values[0]);
        } else {
            timeAndNetwork.append(getString(R.string.show_noairtime));
        }
        final String network = show.getString(ShowQuery.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            timeAndNetwork.append(" ").append(getString(R.string.show_network)).append(" ")
                    .append(network);
        }
        ((TextView) getActivity().findViewById(R.id.showmeta)).setText(timeAndNetwork.toString());
    }
}
