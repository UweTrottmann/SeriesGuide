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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.extensions.ActionsFragmentContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsHelper;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.loaders.EpisodeActionsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FetchArtTask;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import de.greenrobot.event.EventBus;
import java.util.Date;
import java.util.List;
import timber.log.Timber;

/**
 * Displays general information about a show and its next episode.
 */
public class OverviewFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ActionsFragmentContract {

    private static final String TAG = "Overview";

    private static final int EPISODE_LOADER_ID = 100;
    private static final int SHOW_LOADER_ID = 101;
    private static final int ACTIONS_LOADER_ID = 102;

    private static final String KEY_EPISODE_TVDB_ID = "episodeTvdbId";

    private Handler mHandler = new Handler();

    private FetchArtTask mArtTask;

    private Cursor mCurrentEpisodeCursor;
    private int mCurrentEpisodeTvdbId;

    private Cursor mShowCursor;
    private String mShowTitle;

    private View mContainerShow;
    private View mSpacerShow;
    private LinearLayout mContainerActions;

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
        View v = inflater.inflate(R.layout.overview_fragment, container, false);
        v.findViewById(R.id.imageViewFavorite).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleShowFavorited(v);
                fireTrackerEvent("Toggle favorited");
            }
        });
        mContainerShow = v.findViewById(R.id.containerOverviewShow);
        mSpacerShow = v.findViewById(R.id.spacerOverviewShow);
        mContainerActions = (LinearLayout) v.findViewById(R.id.containerEpisodeActions);

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
        mSpacerShow.setVisibility(multiPane ? View.VISIBLE : View.GONE);

        getLoaderManager().initLoader(SHOW_LOADER_ID, null, this);
        getLoaderManager().initLoader(EPISODE_LOADER_ID, null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        loadEpisodeActionsDelayed();
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
        if (mHandler != null) {
            mHandler.removeCallbacks(mEpisodeActionsRunnable);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.overview_fragment_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // enable/disable menu items
        boolean isEpisodeVisible;
        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
            isEpisodeVisible = true;
        } else {
            isEpisodeVisible = false;
        }
        menu.findItem(R.id.menu_overview_manage_lists).setEnabled(isEpisodeVisible);
        menu.findItem(R.id.menu_overview_share).setEnabled(isEpisodeVisible);

        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();
        menu.findItem(R.id.menu_overview_manage_lists)
                .setVisible(!isDrawerOpen && isEpisodeVisible);
        menu.findItem(R.id.menu_overview_share).setVisible(!isDrawerOpen && isEpisodeVisible);
        menu.findItem(R.id.menu_overview_search).setVisible(!isDrawerOpen);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_overview_share) {
            // share episode
            shareEpisode();
            return true;
        } else if (itemId == R.id.menu_overview_manage_lists) {
            fireTrackerEvent("Manage lists");
            if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
                ListsDialogFragment.showListsDialog(
                        mCurrentEpisodeCursor.getString(EpisodeQuery._ID),
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

        if (mShowCursor != null && mShowCursor.moveToFirst() && mCurrentEpisodeCursor != null
                && mCurrentEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final String episodeTitle = mCurrentEpisodeCursor.getString(EpisodeQuery.TITLE);
            // add calendar event
            ShareUtils.onAddCalendarEvent(
                    getActivity(),
                    mShowCursor.getString(ShowQuery.SHOW_TITLE),
                    Utils.getNextEpisodeString(getActivity(), seasonNumber, episodeNumber,
                            episodeTitle),
                    mCurrentEpisodeCursor.getLong(EpisodeQuery.FIRST_RELEASE_MS),
                    mShowCursor.getInt(ShowQuery.SHOW_RUNTIME)
            );
        }
    }

    private void onCheckIn() {
        fireTrackerEvent("Check-In");

        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
            int episodeTvdbId = mCurrentEpisodeCursor.getInt(EpisodeQuery._ID);
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
        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
            final int season = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episode = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            new FlagTask(getActivity(), getShowId())
                    .episodeWatched(mCurrentEpisodeCursor.getInt(EpisodeQuery._ID), season,
                            episode, episodeFlag)
                    .execute();
        }
    }

    private void rateOnTrakt() {
        if (mCurrentEpisodeCursor == null || !mCurrentEpisodeCursor.moveToFirst()) {
            return;
        }
        int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        TraktTools.rateEpisode(getActivity(), getFragmentManager(), getShowId(), seasonNumber,
                episodeNumber);

        fireTrackerEvent("Rate (trakt)");
    }

    private void shareEpisode() {
        if (mCurrentEpisodeCursor == null || !mCurrentEpisodeCursor.moveToFirst()) {
            return;
        }
        int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        String episodeTitle = mCurrentEpisodeCursor.getString(EpisodeQuery.TITLE);

        ShareUtils.shareEpisode(getActivity(), getShowId(), seasonNumber, episodeNumber, mShowTitle,
                episodeTitle);

        fireTrackerEvent("Share");
    }

    private void onToggleCollected() {
        fireTrackerEvent("Toggle Collected");
        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
            final int season = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episode = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final boolean isCollected = mCurrentEpisodeCursor.getInt(EpisodeQuery.COLLECTED) == 1;
            new FlagTask(getActivity(), getShowId())
                    .episodeCollected(mCurrentEpisodeCursor.getInt(EpisodeQuery._ID), season,
                            episode,
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

        String[] PROJECTION = new String[] {
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

        int FIRST_RELEASE_MS = 5;

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

        String[] PROJECTION = new String[] {
                Shows._ID, Shows.TITLE, Shows.STATUS, Shows.AIRSTIME, Shows.AIRSDAYOFWEEK,
                Shows.NETWORK, Shows.POSTER, Shows.IMDBID, Shows.RUNTIME, Shows.FAVORITE,
                Shows.RELEASE_COUNTRY
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_RELEASE_TIME = 3;
        int SHOW_RELEASE_DAY = 4;
        int SHOW_NETWORK = 5;
        int SHOW_POSTER = 6;
        int SHOW_IMDBID = 7;
        int SHOW_RUNTIME = 8;
        int SHOW_FAVORITE = 9;
        int SHOW_RELEASE_COUNTRY = 10;
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
                mCurrentEpisodeCursor = null;
                break;
            case SHOW_LOADER_ID:
                mShowCursor = null;
                break;
        }
    }

    @Override
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        if (mCurrentEpisodeTvdbId == event.episodeTvdbId) {
            loadEpisodeActionsDelayed();
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    @SuppressLint("NewApi")
    private void onPopulateEpisodeData(Cursor episode) {
        mCurrentEpisodeCursor = episode;

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
            mCurrentEpisodeTvdbId = episode.getInt(EpisodeQuery._ID);
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
            long releaseTime = episode.getLong(EpisodeQuery.FIRST_RELEASE_MS);
            if (releaseTime != -1) {
                Date actualRelease = TimeTools.getEpisodeReleaseTime(getActivity(), releaseTime);
                // "in 14 mins (Fri)"
                episodeTime.setText(getString(R.string.release_date_and_day,
                        TimeTools.formatToRelativeLocalReleaseTime(getActivity(), actualRelease),
                        TimeTools.formatToLocalReleaseDay(actualRelease)));
                episodeTime.setVisibility(View.VISIBLE);
            }

            // make title and image clickable
            episodePrimaryContainer.setOnClickListener(new OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onClick(View view) {
                    // display episode details
                    Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                    intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                            mCurrentEpisodeTvdbId);
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
            CheatSheet.setup(collectedButton, isCollected
                    ? R.string.action_collection_remove : R.string.action_collection_add);

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
            menuButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.getMenuInflater().inflate(R.menu.episode_overflow_menu,
                            popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(android.view.MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_action_episode_calendar:
                                    onAddCalendarEvent();
                                    return true;
                            }
                            return false;
                        }
                    });
                    popupMenu.show();
                }
            });

            ratings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rateOnTrakt();
                }
            });
            ratings.setFocusable(true);
            CheatSheet.setup(ratings, R.string.menu_rate_episode);

            // load all other info
            onLoadEpisodeDetails(episode);

            // episode image
            onLoadImage(episode.getString(EpisodeQuery.IMAGE));

            // episode actions
            loadEpisodeActionsDelayed();

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

    @Override
    public void loadEpisodeActions() {
        if (mCurrentEpisodeTvdbId == 0) {
            // do not load actions if there is no episode
            return;
        }
        Bundle args = new Bundle();
        args.putInt(KEY_EPISODE_TVDB_ID, mCurrentEpisodeTvdbId);
        getLoaderManager().restartLoader(ACTIONS_LOADER_ID, args, mEpisodeActionsLoaderCallbacks);
    }

    Runnable mEpisodeActionsRunnable = new Runnable() {
        @Override
        public void run() {
            loadEpisodeActions();
        }
    };

    @Override
    public void loadEpisodeActionsDelayed() {
        mHandler.removeCallbacks(mEpisodeActionsRunnable);
        mHandler.postDelayed(mEpisodeActionsRunnable,
                ActionsFragmentContract.ACTION_LOADER_DELAY_MILLIS);
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
                        .splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS))
        );

        // TVDb rating
        final String ratingText = episode.getString(EpisodeQuery.RATING);
        if (ratingText != null && ratingText.length() != 0) {
            ((TextView) getView().findViewById(R.id.textViewRatingsTvdbValue)).setText(ratingText);
        }

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
        getView().findViewById(R.id.buttonWebSearch).setVisibility(View.GONE);

        // trakt shouts button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
                    Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                    i.putExtras(TraktShoutsActivity.createInitBundleEpisode(getShowId(),
                            seasonNumber, episodeNumber, episodeTitle));
                    startActivity(i);
                    fireTrackerEvent("Comments");
                }
            }
        });
    }

    private void onLoadImage(String imagePath) {
        final FrameLayout container = (FrameLayout) getView().findViewById(R.id.imageContainer);

        // clean up a previous task
        if (mArtTask != null) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
        mArtTask = new FetchArtTask(imagePath, container, getActivity());
        AndroidUtils.executeAsyncTask(mArtTask, new Void[] {
                null
        });
    }

    private void onPopulateShowData(Cursor show) {
        if (show == null || !show.moveToFirst()) {
            return;
        }
        mShowCursor = show;

        // set show title in action bar
        mShowTitle = show.getString(ShowQuery.SHOW_TITLE);
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
        final long releaseTime = show.getLong(ShowQuery.SHOW_RELEASE_TIME);
        final String releaseCountry = show.getString(ShowQuery.SHOW_RELEASE_COUNTRY);
        final String releaseDay = show.getString(ShowQuery.SHOW_RELEASE_DAY);
        if (!TextUtils.isEmpty(releaseDay) && releaseTime != -1) {
            String[] values = TimeTools.formatToShowReleaseTimeAndDay(getActivity(),
                    releaseTime, releaseCountry, releaseDay);
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

    private LoaderManager.LoaderCallbacks<List<Action>> mEpisodeActionsLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<Action>>() {
                @Override
                public Loader<List<Action>> onCreateLoader(int id, Bundle args) {
                    int episodeTvdbId = args.getInt(KEY_EPISODE_TVDB_ID);
                    return new EpisodeActionsLoader(getActivity(), episodeTvdbId);
                }

                @Override
                public void onLoadFinished(Loader<List<Action>> loader, List<Action> data) {
                    if (data == null) {
                        Timber.e("onLoadFinished: did not receive valid actions");
                    } else {
                        Timber.d("onLoadFinished: received " + data.size() + " actions");
                    }
                    EpisodeActionsHelper.populateEpisodeActions(getActivity().getLayoutInflater(),
                            mContainerActions,
                            data);
                }

                @Override
                public void onLoaderReset(Loader<List<Action>> loader) {
                    EpisodeActionsHelper.populateEpisodeActions(getActivity().getLayoutInflater(),
                            mContainerActions,
                            null);
                }
            };
}
