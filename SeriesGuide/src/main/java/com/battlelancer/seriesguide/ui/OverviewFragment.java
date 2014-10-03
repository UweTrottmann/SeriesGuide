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
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.extensions.ActionsFragmentContract;
import com.battlelancer.seriesguide.extensions.EpisodeActionsHelper;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.loaders.EpisodeActionsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.squareup.picasso.Callback;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import de.greenrobot.event.EventBus;
import java.util.Date;
import java.util.List;
import timber.log.Timber;

/**
 * Displays general information about a show and its next episode.
 */
public class OverviewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ActionsFragmentContract {

    private static final String TAG = "Overview";

    private static final String KEY_EPISODE_TVDB_ID = "episodeTvdbId";

    private Handler mHandler = new Handler();

    private TraktSummaryTask mTraktTask;

    private Cursor mCurrentEpisodeCursor;
    private int mCurrentEpisodeTvdbId;

    private Cursor mShowCursor;
    private String mShowTitle;

    private View mContainerShow;
    private View mContainerEpisode;
    private LinearLayout mContainerActions;
    private ImageView mBackgroundImage;
    private ImageView mEpisodeImage;

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
        View v = inflater.inflate(R.layout.fragment_overview, container, false);
        v.findViewById(R.id.imageViewFavorite).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleShowFavorited(v);
                fireTrackerEvent("Toggle favorited");
            }
        });
        mContainerShow = v.findViewById(R.id.containerOverviewShow);
        mContainerEpisode = v.findViewById(R.id.containerOverviewEpisode);
        mContainerEpisode.setVisibility(View.GONE);
        mContainerActions = (LinearLayout) v.findViewById(R.id.containerEpisodeActions);

        mBackgroundImage = (ImageView) v.findViewById(R.id.background);

        mEpisodeImage = (ImageView) v.findViewById(R.id.imageViewOverviewEpisode);

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

        getLoaderManager().initLoader(OverviewActivity.OVERVIEW_SHOW_LOADER_ID, null, this);
        getLoaderManager().initLoader(OverviewActivity.OVERVIEW_EPISODE_LOADER_ID, null, this);

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
    public void onDestroyView() {
        super.onDestroyView();

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        ServiceUtils.getPicasso(getActivity()).cancelRequest(mEpisodeImage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTraktTask != null) {
            mTraktTask.cancel(true);
            mTraktTask = null;
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

        // If no episode is visible, hide actions related to the episode
        boolean isEpisodeVisible = mCurrentEpisodeCursor != null
                && mCurrentEpisodeCursor.moveToFirst();

        // If the nav drawer is open, hide action items related to the content view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();

        // enable/disable menu items
        MenuItem itemShare = menu.findItem(R.id.menu_overview_share);
        itemShare.setEnabled(isEpisodeVisible);
        itemShare.setVisible(!isDrawerOpen && isEpisodeVisible);
        MenuItem itemCalendar = menu.findItem(R.id.menu_overview_calendar);
        itemCalendar.setEnabled(isEpisodeVisible);
        itemCalendar.setVisible(isEpisodeVisible);
        MenuItem itemManageLists = menu.findItem(R.id.menu_overview_manage_lists);
        if (itemManageLists != null) {
            itemManageLists.setEnabled(isEpisodeVisible);
            itemManageLists.setVisible(isEpisodeVisible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_overview_share) {
            shareEpisode();
            return true;
        } else if (itemId == R.id.menu_overview_calendar) {
            createCalendarEvent();
            return true;
        } else if (itemId == R.id.menu_overview_manage_lists) {
            fireTrackerEvent("Manage lists");
            if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
                ManageListsDialogFragment.showListsDialog(
                        mCurrentEpisodeCursor.getInt(EpisodeQuery._ID),
                        ListItemTypes.EPISODE, getFragmentManager());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void createCalendarEvent() {
        fireTrackerEvent("Add to calendar");

        if (mShowCursor != null && mShowCursor.moveToFirst() && mCurrentEpisodeCursor != null
                && mCurrentEpisodeCursor.moveToFirst()) {
            final int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            final int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            final String episodeTitle = mCurrentEpisodeCursor.getString(EpisodeQuery.TITLE);
            // add calendar event
            ShareUtils.suggestCalendarEvent(
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
            EpisodeTools.episodeWatched(getActivity(), getShowId(),
                    mCurrentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, episodeFlag);
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
            EpisodeTools.episodeCollected(getActivity(), getShowId(),
                    mCurrentEpisodeCursor.getInt(EpisodeQuery._ID), season, episode, !isCollected);
        }
    }

    private void onToggleShowFavorited(View v) {
        if (v.getTag() == null) {
            return;
        }

        // store new value
        boolean isFavorite = (Boolean) v.getTag();
        ShowTools.get(getActivity()).storeIsFavorite(getShowId(), !isFavorite);
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
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
            default:
                return new EpisodeLoader(getActivity(), getShowId());
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                return new CursorLoader(getActivity(), Shows.buildShowUri(String
                        .valueOf(getShowId())), ShowQuery.PROJECTION, null, null, null);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (isAdded()) {
            switch (loader.getId()) {
                case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
                    getActivity().invalidateOptionsMenu();
                    onPopulateEpisodeData(data);
                    break;
                case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
                    onPopulateShowData(data);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case OverviewActivity.OVERVIEW_EPISODE_LOADER_ID:
                mCurrentEpisodeCursor = null;
                break;
            case OverviewActivity.OVERVIEW_SHOW_LOADER_ID:
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

    public void onEventMainThread(TraktActionCompleteEvent event) {
        if (event.mTraktAction == TraktAction.RATE_EPISODE) {
            onLoadTraktRatings(false);
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
        final TextView episodeSeasonAndNumber = (TextView) getView().findViewById(R.id.episodeInfo);
        final View episodemeta = getView().findViewById(R.id.episode_meta_container);
        final View episodePrimaryContainer = getView().findViewById(R.id.episode_primary_container);
        final View buttons = getView().findViewById(R.id.buttonbar);
        final View ratings = getView().findViewById(R.id.ratingbar);

        if (episode != null && episode.moveToFirst()) {
            // some episode properties
            mCurrentEpisodeTvdbId = episode.getInt(EpisodeQuery._ID);

            // title
            episodeTitle.setText(episode.getString(EpisodeQuery.TITLE));

            // number
            StringBuilder infoText = new StringBuilder();
            infoText.append(getString(R.string.season_number, episode.getInt(EpisodeQuery.SEASON)));
            infoText.append(" ");
            int episodeNumber = episode.getInt(EpisodeQuery.NUMBER);
            infoText.append(getString(R.string.episode_number, episodeNumber));
            int episodeAbsoluteNumber = episode.getInt(EpisodeQuery.ABSOLUTE_NUMBER);
            if (episodeAbsoluteNumber > 0 && episodeAbsoluteNumber != episodeNumber) {
                infoText.append(" (").append(episodeAbsoluteNumber).append(")");
            }
            episodeSeasonAndNumber.setText(infoText);

            // air date
            long releaseTime = episode.getLong(EpisodeQuery.FIRST_RELEASE_MS);
            if (releaseTime != -1) {
                Date actualRelease = TimeTools.getEpisodeReleaseTime(getActivity(), releaseTime);
                // "in 14 mins (Fri)"
                episodeTime.setText(getString(R.string.release_date_and_day,
                        TimeTools.formatToRelativeLocalReleaseTime(getActivity(), actualRelease),
                        TimeTools.formatToLocalReleaseDay(actualRelease)));
            } else {
                episodeTime.setText(null);
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

                    ActivityCompat.startActivity(getActivity(), intent,
                            ActivityOptionsCompat.makeScaleUpAnimation(view, 0, 0, view.getWidth(),
                                    view.getHeight()).toBundle()
                    );
                }
            });
            episodePrimaryContainer.setFocusable(true);

            // Button bar
            // check-in button
            View checkinButton = buttons.findViewById(R.id.buttonEpisodeCheckin);
            checkinButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCheckIn();
                }
            });
            CheatSheet.setup(checkinButton);

            // watched button
            View watchedButton = buttons.findViewById(R.id.buttonEpisodeWatched);
            watchedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disable button, will be re-enabled on data reload once action completes
                    v.setEnabled(false);
                    onEpisodeWatched();
                }
            });
            watchedButton.setEnabled(true);
            CheatSheet.setup(watchedButton);

            // collected button
            boolean isCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1;
            Button collectedButton = (Button) buttons.findViewById(R.id.buttonEpisodeCollected);
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(collectedButton, 0,
                    isCollected ? R.drawable.ic_collected
                            : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                    R.attr.drawableCollect), 0, 0);
            collectedButton.setText(isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);
            CheatSheet.setup(collectedButton, isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);
            collectedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disable button, will be re-enabled on data reload once action completes
                    v.setEnabled(false);
                    onToggleCollected();
                }
            });
            collectedButton.setEnabled(true);

            // skip button
            View skipButton = buttons.findViewById(R.id.buttonEpisodeSkip);
            skipButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disable button, will be re-enabled on data reload once action completes
                    v.setEnabled(false);
                    onEpisodeSkipped();
                }
            });
            skipButton.setEnabled(true);
            CheatSheet.setup(skipButton);

            // ratings
            ratings.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rateOnTrakt();
                }
            });
            ratings.setFocusable(true);
            CheatSheet.setup(ratings, R.string.action_rate);

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
            episodeTime.setText(null);
            episodeSeasonAndNumber.setText(null);
            episodePrimaryContainer.setOnClickListener(null);
            episodePrimaryContainer.setClickable(false);
            episodePrimaryContainer.setFocusable(false);
            episodemeta.setVisibility(View.GONE);
            ratings.setOnClickListener(null);
            ratings.setClickable(false);
            ratings.setFocusable(false);
            onLoadImage(null);
        }

        // enable/disable applicable menu items
        getActivity().invalidateOptionsMenu();

        // animate view into visibility
        if (mContainerEpisode.getVisibility() == View.GONE) {
            final View progressContainer = getView().findViewById(R.id.progress_container);
            progressContainer.startAnimation(AnimationUtils
                    .loadAnimation(episodemeta.getContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            mContainerEpisode.startAnimation(AnimationUtils
                    .loadAnimation(episodemeta.getContext(), android.R.anim.fade_in));
            mContainerEpisode.setVisibility(View.VISIBLE);
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
        getLoaderManager().restartLoader(OverviewActivity.OVERVIEW_ACTIONS_LOADER_ID, args,
                mEpisodeActionsLoaderCallbacks);
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

        boolean isShowingMeta;
        isShowingMeta = Utils.setLabelValueOrHide(getView().findViewById(R.id.labelDvd),
                (TextView) getView().findViewById(R.id.textViewEpisodeDVDnumber), episode
                        .getDouble(EpisodeQuery.DVDNUMBER));
        isShowingMeta |= Utils.setLabelValueOrHide(getView().findViewById(R.id.labelGuestStars),
                (TextView) getView().findViewById(R.id.TextViewEpisodeGuestStars), Utils
                        .splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS))
        );
        // hide divider if no meta is visible
        getView().findViewById(R.id.dividerHorizontalOverviewEpisodeMeta)
                .setVisibility(isShowingMeta ? View.VISIBLE : View.GONE);

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
                    ActivityCompat.startActivity(getActivity(), i,
                            ActivityOptionsCompat
                                    .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                    .toBundle()
                    );
                    fireTrackerEvent("Comments");
                }
            }
        });

        // trakt ratings
        onLoadTraktRatings(true);
    }

    private void onLoadTraktRatings(boolean isUseCachedValues) {
        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()
                && (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED)) {
            int episodeTvdbId = mCurrentEpisodeCursor.getInt(EpisodeQuery._ID);
            int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            mTraktTask = new TraktSummaryTask(getActivity(), getView(), isUseCachedValues)
                    .episode(getShowId(), episodeTvdbId, seasonNumber, episodeNumber);
            AndroidUtils.executeOnPool(mTraktTask);
        }
    }

    private void onLoadImage(String imagePath) {
        // immediately hide container if there is no image
        if (TextUtils.isEmpty(imagePath)) {
            mEpisodeImage.setVisibility(View.INVISIBLE);
            return;
        }

        // try loading image
        mEpisodeImage.setVisibility(View.VISIBLE);
        ServiceUtils.getPicasso(getActivity()).load(TheTVDB.buildScreenshotUrl(imagePath))
                .error(R.drawable.ic_image_missing)
                .into(mEpisodeImage,
                        new Callback() {
                            @Override
                            public void onSuccess() {
                                mEpisodeImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }

                            @Override
                            public void onError() {
                                mEpisodeImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            }
                        }
                );
    }

    private void onPopulateShowData(Cursor show) {
        if (show == null || !show.moveToFirst()) {
            return;
        }
        mShowCursor = show;

        // set show title in action bar
        mShowTitle = show.getString(ShowQuery.SHOW_TITLE);
        final ActionBar actionBar = getActivity().getActionBar();
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

        // poster background
        Utils.loadPosterBackground(getActivity(), mBackgroundImage,
                show.getString(ShowQuery.SHOW_POSTER));

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
