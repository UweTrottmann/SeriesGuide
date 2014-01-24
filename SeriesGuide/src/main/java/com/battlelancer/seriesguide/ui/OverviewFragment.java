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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.ShareUtils.ShareMethod;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import de.greenrobot.event.EventBus;

/**
 * Displays general information about a show and its next episode.
 */
public class OverviewFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "Overview";

    private static final int EPISODE_LOADER_ID = 100;

    private static final int SHOW_LOADER_ID = 101;

    private static final int CONTEXT_CREATE_CALENDAR_EVENT_ID = 201;

    private FetchArtTask mArtTask;

    private TraktSummaryTask mTraktTask;

    private Cursor mEpisodeCursor;

    private Cursor mShowCursor;

    private String mShowTitle;

    private View mContainerShow;

    private View mDividerShow;

    private View mSpacerShow;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";
    }

    public static OverviewFragment newInstance(int showId) {
        OverviewFragment f = new OverviewFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        View v = inflater.inflate(R.layout.overview_fragment, container, false);
        v.findViewById(R.id.imageViewFavorite).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleShowFavorited(v);
                fireTrackerEvent("Toggle favorited");
            }
        });
        mContainerShow = v.findViewById(R.id.containerOverviewShow);
        mDividerShow = v.findViewById(R.id.dividerHorizontalOverviewShow);
        mSpacerShow = v.findViewById(R.id.spacerOverviewShow);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Are we in a multi-pane layout?
        View seasonsFragment = getActivity().findViewById(R.id.fragment_seasons);
        boolean multiPane = seasonsFragment != null
                && seasonsFragment.getVisibility() == View.VISIBLE;

        // do not display show info header in multi pane layout
        mContainerShow.setVisibility(multiPane ? View.GONE : View.VISIBLE);
        mDividerShow.setVisibility(multiPane ? View.GONE : View.VISIBLE);
        mSpacerShow.setVisibility(multiPane ? View.VISIBLE : View.GONE);

        getLoaderManager().initLoader(SHOW_LOADER_ID, null, this);
        getLoaderManager().initLoader(EPISODE_LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_CREATE_CALENDAR_EVENT_ID, 0, R.string.addtocalendar);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case CONTEXT_CREATE_CALENDAR_EVENT_ID: {
                onAddCalendarEvent();
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.SeriesGuideThemeLight;
        inflater.inflate(
                isLightTheme ? R.menu.overview_fragment_menu_light : R.menu.overview_fragment_menu,
                menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // enable/disable menu items
        boolean isEpisodeVisible;
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            isEpisodeVisible = true;
        } else {
            isEpisodeVisible = false;
        }
        menu.findItem(R.id.menu_overview_manage_lists).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_overview_share).setEnabled(isEpisodeVisible);

        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();
        menu.findItem(R.id.menu_overview_manage_lists).setVisible(!isDrawerOpen);
        menu.findItem(R.id.menu_overview_share).setVisible(!isDrawerOpen);
        menu.findItem(R.id.menu_overview_search).setVisible(!isDrawerOpen);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_overview_share) {
            // share episode
            fireTrackerEvent("Share");
            onShareEpisode(ShareMethod.OTHER_SERVICES);
            return true;
        } else if (itemId == R.id.menu_overview_manage_lists) {
            fireTrackerEvent("Manage lists");
            if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
                ListsDialogFragment.showListsDialog(mEpisodeCursor.getString(EpisodeQuery._ID),
                        ListItemTypes.EPISODE, getFragmentManager());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onAddCalendarEvent() {
        fireTrackerEvent("Add to calendar");

        if (mShowCursor != null && mShowCursor.moveToFirst() && mEpisodeCursor != null
                && mEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final String episodeTitle = mEpisodeCursor.getString(EpisodeQuery.TITLE);
            // add calendar event
            ShareUtils.onAddCalendarEvent(
                    getActivity(),
                    mShowCursor.getString(ShowQuery.SHOW_TITLE),
                    Utils.getNextEpisodeString(getActivity(), seasonNumber, episodeNumber,
                            episodeTitle), mEpisodeCursor.getLong(EpisodeQuery.FIRSTAIREDMS),
                    mShowCursor.getInt(ShowQuery.SHOW_RUNTIME));
        }
    }

    private void onCheckIn() {
        fireTrackerEvent("Check-In");

        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            int episodeTvdbId = mEpisodeCursor.getInt(EpisodeQuery._ID);
            // check in
            CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(),
                    episodeTvdbId);
            f.show(getFragmentManager(), "checkin-dialog");
        }
    }

    private void onEpisodeSkipped() {
        onChangeEpisodeFlag(EpisodeFlags.SKIPPED);
        fireTrackerEvent("Flag Skipped");
    }

    private void onEpisodeWatched() {
        onChangeEpisodeFlag(EpisodeFlags.WATCHED);
        fireTrackerEvent("Flag Watched");
    }

    private void onChangeEpisodeFlag(int episodeFlag) {
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            final int season = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episode = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            new FlagTask(getActivity(), getShowId())
                    .episodeWatched(mEpisodeCursor.getInt(EpisodeQuery._ID), season,
                            episode, episodeFlag)
                    .execute();
        }
    }

    private void onRateOnTrakt() {
        // rate episode on trakt.tv
        if (TraktCredentials.ensureCredentials(getActivity())) {
            onShareEpisode(ShareMethod.RATE_TRAKT);
        }
        fireTrackerEvent("Rate (trakt)");
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

            String episodestring = Utils.getNextEpisodeString(getActivity(), seasonNumber,
                    episodeNumber, mEpisodeCursor.getString(EpisodeQuery.TITLE));
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

            ShareUtils.onShareEpisode(getActivity(), shareData, shareMethod);
        }
    }

    private void onToggleCollected() {
        fireTrackerEvent("Toggle Collected");
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
            final int season = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episode = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final boolean isCollected = mEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1;
            new FlagTask(getActivity(), getShowId())
                    .episodeCollected(mEpisodeCursor.getInt(EpisodeQuery._ID), season, episode,
                            !isCollected)
                    .execute();
        }
    }

    private void onToggleShowFavorited(View v) {
        if (v.getTag() == null) {
            return;
        }

        // store new value
        boolean isFavorite = (Boolean) v.getTag();
        ShowTools.get(getActivity()).storeIsFavorite(getShowId(), !isFavorite);

        // favoriting makes show eligible for notifications
        Utils.runNotificationService(getActivity());
    }

    public static class EpisodeLoader extends CursorLoader {

        private int mShowTvdbId;

        public EpisodeLoader(Context context, int showTvdbId) {
            super(context);
            mShowTvdbId = showTvdbId;
            setProjection(EpisodeQuery.PROJECTION);
        }

        @Override
        public Cursor loadInBackground() {
            // get episode id, set query params
            int episodeId = (int) DBUtils.updateLatestEpisode(getContext(), mShowTvdbId);
            setUri(Episodes.buildEpisodeUri(episodeId));

            return super.loadInBackground();
        }

    }

    interface EpisodeQuery {

        String[] PROJECTION = new String[]{
                Episodes._ID, Episodes.OVERVIEW, Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED,
                Episodes.FIRSTAIREDMS, Episodes.GUESTSTARS, Episodes.RATING, Episodes.IMAGE,
                Episodes.DVDNUMBER, Episodes.TITLE, Seasons.REF_SEASON_ID, Episodes.COLLECTED,
                Episodes.IMDBID, Episodes.ABSOLUTE_NUMBER

        };

        int _ID = 0;

        int OVERVIEW = 1;

        int NUMBER = 2;

        int SEASON = 3;

        int WATCHED = 4;

        int FIRSTAIREDMS = 5;

        int GUESTSTARS = 6;

        int RATING = 7;

        int IMAGE = 8;

        int DVDNUMBER = 9;

        int TITLE = 10;

        int REF_SEASON_ID = 11;

        int COLLECTED = 12;

        int IMDBID = 13;

        int ABSOLUTE_NUMBER = 14;

    }

    interface ShowQuery {

        String[] PROJECTION = new String[]{
                Shows._ID, Shows.TITLE, Shows.STATUS, Shows.AIRSTIME, Shows.AIRSDAYOFWEEK,
                Shows.NETWORK, Shows.POSTER, Shows.IMDBID, Shows.RUNTIME, Shows.FAVORITE
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_AIRSTIME = 3;
        int SHOW_AIRSDAYOFWEEK = 4;
        int SHOW_NETWORK = 5;
        int SHOW_POSTER = 6;
        int SHOW_IMDBID = 7;
        int SHOW_RUNTIME = 8;
        int SHOW_FAVORITE = 9;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case EPISODE_LOADER_ID:
            default:
                return new EpisodeLoader(getActivity(), getShowId());
            case SHOW_LOADER_ID:
                return new CursorLoader(getActivity(), Shows.buildShowUri(String
                        .valueOf(getShowId())), ShowQuery.PROJECTION, null, null, null);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (isAdded()) {
            switch (loader.getId()) {
                case EPISODE_LOADER_ID:
                    getSherlockActivity().invalidateOptionsMenu();
                    onPopulateEpisodeData(data);
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

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktAction == TraktAction.RATE_EPISODE) {
            onLoadTraktRatings(false);
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    @SuppressLint("NewApi")
    private void onPopulateEpisodeData(Cursor episode) {
        mEpisodeCursor = episode;

        final TextView episodeTitle = (TextView) getView().findViewById(R.id.episodeTitle);
        final TextView episodeTime = (TextView) getView().findViewById(R.id.episodeTime);
        final TextView episodeInfo = (TextView) getView().findViewById(R.id.episodeInfo);
        final View episodemeta = getView().findViewById(R.id.episode_meta_container);
        final View episodePrimaryContainer = getView().findViewById(R.id.episode_primary_container);
        final View buttons = getView().findViewById(R.id.buttonbar);
        final View ratings = getView().findViewById(R.id.ratingbar);

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
            infoText.append(getString(R.string.season_number, seasonNumber));
            infoText.append(" ");
            infoText.append(getString(R.string.episode_number, episodeNumber));
            if (episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != episodeNumber) {
                infoText.append(" (").append(episodeAbsoluteNumber).append(")");
            }
            episodeInfo.setText(infoText);
            episodeInfo.setVisibility(View.VISIBLE);

            // air date
            long airtime = episode.getLong(EpisodeQuery.FIRSTAIREDMS);
            if (airtime != -1) {
                final String[] dayAndTime = Utils.formatToTimeAndDay(airtime, getActivity());
                episodeTime.setText(
                        getString(R.string.release_date_and_day, dayAndTime[2], dayAndTime[1]));
                episodeTime.setVisibility(View.VISIBLE);
            }

            // make title and image clickable
            episodePrimaryContainer.setOnClickListener(new OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onClick(View view) {
                    // display episode details
                    Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.blow_up_enter,
                            R.anim.blow_up_exit);
                }
            });
            episodePrimaryContainer.setFocusable(true);

            // Button bar
            // check-in button
            View checkinButton = buttons.findViewById(R.id.imageButtonBarCheckin);
            checkinButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCheckIn();
                }
            });
            CheatSheet.setup(checkinButton);

            // watched button
            View watchedButton = buttons.findViewById(R.id.imageButtonBarWatched);
            watchedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEpisodeWatched();
                }
            });
            CheatSheet.setup(watchedButton, R.string.mark_episode);

            // collected button
            boolean isCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1;
            ImageButton collectedButton = (ImageButton) buttons
                    .findViewById(R.id.imageButtonBarCollected);
            collectedButton.setImageResource(isCollected ? R.drawable.ic_collected
                    : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableCollect));
            collectedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToggleCollected();
                }
            });
            CheatSheet.setup(collectedButton, isCollected ? R.string.uncollect
                    : R.string.collect);

            // skip button
            View skipButton = buttons.findViewById(R.id.imageButtonBarSkip);
            skipButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEpisodeSkipped();
                }
            });
            CheatSheet.setup(skipButton);

            // button bar menu
            View menuButton = buttons.findViewById(R.id.imageButtonBarMenu);
            registerForContextMenu(menuButton);
            menuButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().openContextMenu(v);
                }
            });

            ratings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRateOnTrakt();
                }
            });
            ratings.setFocusable(true);
            CheatSheet.setup(ratings, R.string.menu_rate_episode);

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
            episodePrimaryContainer.setOnClickListener(null);
            episodePrimaryContainer.setClickable(false);
            episodePrimaryContainer.setFocusable(false);
            buttons.setVisibility(View.GONE);
            ratings.setOnClickListener(null);
            ratings.setClickable(false);
            ratings.setFocusable(false);
            onLoadImage(null);
        }

        // enable/disable applicable menu items
        getSherlockActivity().invalidateOptionsMenu();

        // animate view into visibility
        final View contentContainer = getView().findViewById(R.id.content_container);
        if (contentContainer.getVisibility() == View.GONE) {
            final View progressContainer = getView().findViewById(R.id.progress_container);
            progressContainer.startAnimation(AnimationUtils
                    .loadAnimation(episodemeta.getContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            contentContainer.startAnimation(AnimationUtils
                    .loadAnimation(episodemeta.getContext(), android.R.anim.fade_in));
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
                .getDouble(EpisodeQuery.DVDNUMBER));
        Utils.setLabelValueOrHide(getView().findViewById(R.id.labelGuestStars),
                (TextView) getView().findViewById(R.id.TextViewEpisodeGuestStars), Utils
                .splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));

        // TVDb rating
        final String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            ((TextView) getView().findViewById(R.id.textViewRatingsTvdbValue)).setText(ratingText);
        }

        // Google Play button
        View playButton = getView().findViewById(R.id.buttonGooglePlay);
        ServiceUtils.setUpGooglePlayButton(mShowTitle + " " + episodeTitle, playButton, TAG);

        // Amazon button
        View amazonButton = getView().findViewById(R.id.buttonAmazon);
        ServiceUtils.setUpAmazonButton(mShowTitle + " " + episodeTitle, amazonButton, TAG);

        // YouTube button
        View youtubeButton = getView().findViewById(R.id.buttonYouTube);
        ServiceUtils.setUpYouTubeButton(mShowTitle + " " + episodeTitle, youtubeButton, TAG);

        // IMDb button
        String imdbId = episode.getString(EpisodeQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId) && mShowCursor != null) {
            // fall back to show IMDb id
            imdbId = mShowCursor.getString(ShowQuery.SHOW_IMDBID);
        }
        ServiceUtils.setUpImdbButton(imdbId, getView().findViewById(R.id.buttonShowInfoIMDB), TAG,
                getActivity());

        // TVDb button
        final int episodeTvdbId = episode.getInt(EpisodeQuery._ID);
        final int seasonTvdbId = episode.getInt(EpisodeQuery.REF_SEASON_ID);
        ServiceUtils.setUpTvdbButton(getShowId(), seasonTvdbId, episodeTvdbId, getView()
                .findViewById(R.id.buttonTVDB), TAG);

        // trakt button
        ServiceUtils.setUpTraktButton(getShowId(), seasonNumber, episodeNumber, getView()
                .findViewById(R.id.buttonTrakt), TAG);

        // Web search button
        View webSearch = getView().findViewById(R.id.buttonWebSearch);
        ServiceUtils.setUpWebSearchButton(mShowTitle + " " + episodeTitle, webSearch, TAG);

        // trakt shouts button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()) {
                    Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                    i.putExtras(TraktShoutsActivity.createInitBundleEpisode(getShowId(),
                            seasonNumber, episodeNumber, episodeTitle));
                    startActivity(i);
                    fireTrackerEvent("Comments");
                }
            }
        });

        // trakt ratings
        onLoadTraktRatings(true);
    }

    private void onLoadTraktRatings(boolean isUseCachedValues) {
        if (mEpisodeCursor != null && mEpisodeCursor.moveToFirst()
                && (mTraktTask == null || mTraktTask.getStatus() != AsyncTask.Status.RUNNING)) {
            int seasonNumber = mEpisodeCursor.getInt(EpisodeQuery.SEASON);
            int episodeNumber = mEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            mTraktTask = new TraktSummaryTask(getSherlockActivity(), getView(), isUseCachedValues)
                    .episode(getShowId(), seasonNumber, episodeNumber);
            AndroidUtils.executeAsyncTask(mTraktTask, new Void[]{});
        }
    }

    private void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getView().findViewById(R.id.imageContainer);

        // clean up a previous task
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        mArtTask = (FetchArtTask) new FetchArtTask(imagePath, container, getActivity());
        AndroidUtils.executeAsyncTask(mArtTask, new Void[]{
                null
        });
    }

    private void onPopulateShowData(Cursor show) {
        if (show == null || !show.moveToFirst()) {
            return;
        }
        mShowCursor = show;

        // title
        mShowTitle = show.getString(ShowQuery.SHOW_TITLE);
        ((TextView) getView().findViewById(R.id.seriesname)).setText(mShowTitle);

        // set show title in action bar
        final ActionBar actionBar = getSherlockActivity().getSupportActionBar();
        actionBar.setTitle(mShowTitle);

        // status
        final TextView statusText = (TextView) getView().findViewById(R.id.showStatus);
        int status = show.getInt(ShowQuery.SHOW_STATUS);
        if (status == 1) {
            statusText.setTextColor(getResources().getColor(Utils.resolveAttributeToResourceId(
                    getActivity().getTheme(), R.attr.textColorSgGreen)));
            statusText.setText(getString(R.string.show_isalive));
        } else if (status == 0) {
            statusText.setTextColor(Color.GRAY);
            statusText.setText(getString(R.string.show_isnotalive));
        }

        // favorite
        final ImageView favorited = (ImageView) getView().findViewById(R.id.imageViewFavorite);
        boolean isFavorited = show.getInt(ShowQuery.SHOW_FAVORITE) == 1;
        if (isFavorited) {
            favorited.setImageResource(Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                    R.attr.drawableStar));
        } else {
            favorited.setImageResource(Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                    R.attr.drawableStar0));
        }
        CheatSheet.setup(favorited, isFavorited ? R.string.context_unfavorite
                : R.string.context_favorite);
        favorited.setTag(isFavorited);

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
            timeAndNetwork.append(values[1])
                    .append(" ")
                    .append(values[0])
                    .append(" ");
        }
        final String network = show.getString(ShowQuery.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            timeAndNetwork.append(getString(R.string.show_on_network, network));
        }
        ((TextView) getActivity().findViewById(R.id.showmeta)).setText(timeAndNetwork.toString());
    }
}
