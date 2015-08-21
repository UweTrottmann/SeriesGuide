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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import butterknife.Bind;
import butterknife.ButterKnife;
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
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
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

    private TraktRatingsTask mTraktTask;

    private Cursor mCurrentEpisodeCursor;
    private int mCurrentEpisodeTvdbId;

    private Cursor mShowCursor;
    private String mShowTitle;

    private View mContainerShow;
    private View mContainerEpisode;
    private LinearLayout mContainerActions;
    private ImageView mBackgroundImage;
    private ImageView mEpisodeImage;

    @Bind(R.id.episodeTitle) TextView textEpisodeTitle;
    @Bind(R.id.episodeTime) TextView textEpisodeTime;
    @Bind(R.id.episodeInfo) TextView textEpisodeNumbers;
    @Bind(R.id.episode_primary_container) View containerEpisodePrimary;
    @Bind(R.id.episode_meta_container) View containerEpisodeMeta;
    @Bind(R.id.dividerHorizontalOverviewEpisodeMeta) View dividerEpisodeMeta;
    @Bind(R.id.progress_container) View containerProgress;
    @Bind(R.id.containerRatings) View containerRatings;
    @Bind(R.id.buttonEpisodeCheckin) Button buttonCheckin;
    @Bind(R.id.buttonEpisodeWatched) Button buttonWatch;
    @Bind(R.id.buttonEpisodeCollected) Button buttonCollect;
    @Bind(R.id.buttonEpisodeSkip) Button buttonSkip;

    @Bind(R.id.TextViewEpisodeDescription) TextView textDescription;
    @Bind(R.id.labelDvd) View labelDvdNumber;
    @Bind(R.id.textViewEpisodeDVDnumber) TextView textDvdNumber;
    @Bind(R.id.labelGuestStars) View labelGuestStars;
    @Bind(R.id.TextViewEpisodeGuestStars) TextView textGuestStars;
    @Bind(R.id.textViewRatingsValue) TextView textRating;
    @Bind(R.id.textViewRatingsVotes) TextView textRatingVotes;
    @Bind(R.id.textViewRatingsUser) TextView textUserRating;

    @Bind(R.id.buttonShowInfoIMDB) View buttonImdb;
    @Bind(R.id.buttonTVDB) View buttonTvdb;
    @Bind(R.id.buttonTrakt) View buttonTrakt;
    @Bind(R.id.buttonWebSearch) View buttonWebSearch;
    @Bind(R.id.buttonShouts) View buttonComments;

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
        ButterKnife.bind(this, v);

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

        // check-in button
        buttonCheckin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCheckIn();
            }
        });
        CheatSheet.setup(buttonCheckin);

        // watched button
        buttonWatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                onEpisodeWatched();
            }
        });
        buttonWatch.setEnabled(true);
        CheatSheet.setup(buttonWatch);

        // collected button
        buttonCollect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                onToggleCollected();
            }
        });

        // skip button
        buttonSkip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                onEpisodeSkipped();
            }
        });
        buttonSkip.setEnabled(true);
        CheatSheet.setup(buttonSkip);

        // ratings
        containerRatings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateEpisode();
            }
        });
        containerRatings.setFocusable(true);
        CheatSheet.setup(containerRatings, R.string.action_rate);

        // hide web search button
        buttonWebSearch.setVisibility(View.GONE);

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

        ButterKnife.unbind(this);
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

        // If no episode is visible, hide actions related to the episode
        boolean isEpisodeVisible = mCurrentEpisodeCursor != null
                && mCurrentEpisodeCursor.moveToFirst();

        // enable/disable menu items
        MenuItem itemShare = menu.findItem(R.id.menu_overview_share);
        itemShare.setEnabled(isEpisodeVisible);
        itemShare.setVisible(isEpisodeVisible);
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
            if (f != null) {
                f.show(getFragmentManager(), "checkin-dialog");
            }
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

    private void rateEpisode() {
        if (mCurrentEpisodeTvdbId == 0) {
            return;
        }

        EpisodeTools.displayRateDialog(getActivity(), getFragmentManager(), mCurrentEpisodeTvdbId);

        fireTrackerEvent("Rate (trakt)");
    }

    private void shareEpisode() {
        if (mCurrentEpisodeCursor == null || !mCurrentEpisodeCursor.moveToFirst()) {
            return;
        }
        int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
        int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
        String episodeTitle = mCurrentEpisodeCursor.getString(EpisodeQuery.TITLE);

        ShareUtils.shareEpisode(getActivity(), mCurrentEpisodeTvdbId, seasonNumber, episodeNumber,
                mShowTitle, episodeTitle);

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
                Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.DVDNUMBER,
                Episodes.SEASON,
                Seasons.REF_SEASON_ID,
                Episodes.IMDBID,
                Episodes.TITLE,
                Episodes.OVERVIEW,
                Episodes.FIRSTAIREDMS,
                Episodes.GUESTSTARS,
                Episodes.RATING_GLOBAL,
                Episodes.RATING_VOTES,
                Episodes.RATING_USER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.IMAGE
        };

        int _ID = 0;
        int NUMBER = 1;
        int ABSOLUTE_NUMBER = 2;
        int DVD_NUMBER = 3;
        int SEASON = 4;
        int SEASON_ID = 5;
        int IMDBID = 6;
        int TITLE = 7;
        int OVERVIEW = 8;
        int FIRST_RELEASE_MS = 9;
        int GUESTSTARS = 10;
        int RATING_GLOBAL = 11;
        int RATING_VOTES = 12;
        int RATING_USER = 13;
        int WATCHED = 14;
        int COLLECTED = 15;
        int IMAGE = 16;
    }

    interface ShowQuery {

        String[] PROJECTION = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.NETWORK,
                Shows.POSTER,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE
        };

        int SHOW_TITLE = 1;
        int SHOW_STATUS = 2;
        int SHOW_RELEASE_TIME = 3;
        int SHOW_RELEASE_WEEKDAY = 4;
        int SHOW_RELEASE_TIMEZONE = 5;
        int SHOW_RELEASE_COUNTRY = 6;
        int SHOW_NETWORK = 7;
        int SHOW_POSTER = 8;
        int SHOW_IMDBID = 9;
        int SHOW_RUNTIME = 10;
        int SHOW_FAVORITE = 11;
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

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private void onPopulateEpisodeData(Cursor episode) {
        mCurrentEpisodeCursor = episode;

        if (episode != null && episode.moveToFirst()) {
            // some episode properties
            mCurrentEpisodeTvdbId = episode.getInt(EpisodeQuery._ID);

            // title
            textEpisodeTitle.setText(episode.getString(EpisodeQuery.TITLE));

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
            textEpisodeNumbers.setText(infoText);

            // air date
            long releaseTime = episode.getLong(EpisodeQuery.FIRST_RELEASE_MS);
            if (releaseTime != -1) {
                Date actualRelease = TimeTools.applyUserOffset(getActivity(), releaseTime);
                // "in 14 mins (Fri)"
                textEpisodeTime.setText(getString(R.string.release_date_and_day,
                        TimeTools.formatToLocalRelativeTime(getActivity(), actualRelease),
                        TimeTools.formatToLocalDay(actualRelease)));
            } else {
                textEpisodeTime.setText(null);
            }

            // make title and image clickable
            containerEpisodePrimary.setOnClickListener(new OnClickListener() {
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
            containerEpisodePrimary.setFocusable(true);

            // collected button
            boolean isCollected = episode.getInt(EpisodeQuery.COLLECTED) == 1;
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(buttonCollect, 0,
                    isCollected ? R.drawable.ic_collected
                            : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                    R.attr.drawableCollect), 0, 0);
            buttonCollect.setText(isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);
            CheatSheet.setup(buttonCollect, isCollected ? R.string.action_collection_remove
                    : R.string.action_collection_add);

            // buttons might have been disabled by action, re-enable
            buttonWatch.setEnabled(true);
            buttonCollect.setEnabled(true);
            buttonSkip.setEnabled(true);

            // load all other info
            onLoadEpisodeDetails(episode);

            // episode image
            onLoadImage(episode.getString(EpisodeQuery.IMAGE));

            // episode actions
            loadEpisodeActionsDelayed();

            containerEpisodeMeta.setVisibility(View.VISIBLE);
        } else {
            // no next episode: display single line info text, remove other
            // views
            textEpisodeTitle.setText(R.string.no_nextepisode);
            textEpisodeTime.setText(null);
            textEpisodeNumbers.setText(null);
            containerEpisodePrimary.setOnClickListener(null);
            containerEpisodePrimary.setClickable(false);
            containerEpisodePrimary.setFocusable(false);
            containerEpisodeMeta.setVisibility(View.GONE);
            onLoadImage(null);
        }

        // enable/disable applicable menu items
        getActivity().invalidateOptionsMenu();

        // animate view into visibility
        if (mContainerEpisode.getVisibility() == View.GONE) {
            containerProgress.startAnimation(AnimationUtils
                    .loadAnimation(containerProgress.getContext(), android.R.anim.fade_out));
            containerProgress.setVisibility(View.GONE);
            mContainerEpisode.startAnimation(AnimationUtils
                    .loadAnimation(mContainerEpisode.getContext(), android.R.anim.fade_in));
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
        // description
        textDescription.setText(episode.getString(EpisodeQuery.OVERVIEW));

        // dvd number
        boolean isShowingMeta = Utils.setLabelValueOrHide(labelDvdNumber, textDvdNumber,
                episode.getDouble(EpisodeQuery.DVD_NUMBER));
        // guest stars
        isShowingMeta |= Utils.setLabelValueOrHide(labelGuestStars, textGuestStars,
                Utils.splitAndKitTVDBStrings(episode.getString(EpisodeQuery.GUESTSTARS)));
        // hide divider if no meta is visible
        dividerEpisodeMeta.setVisibility(isShowingMeta ? View.VISIBLE : View.GONE);

        // trakt rating
        textRating.setText(
                TraktTools.buildRatingString(episode.getDouble(EpisodeQuery.RATING_GLOBAL)));
        textRatingVotes.setText(TraktTools.buildRatingVotesString(getActivity(),
                episode.getInt(EpisodeQuery.RATING_VOTES)));

        // user rating
        textUserRating.setText(TraktTools.buildUserRatingString(getActivity(),
                episode.getInt(EpisodeQuery.RATING_USER)));

        // IMDb button
        String imdbId = episode.getString(EpisodeQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId) && mShowCursor != null) {
            // fall back to show IMDb id
            imdbId = mShowCursor.getString(ShowQuery.SHOW_IMDBID);
        }
        ServiceUtils.setUpImdbButton(imdbId, buttonImdb, TAG);

        // TVDb button
        final int episodeTvdbId = episode.getInt(EpisodeQuery._ID);
        final int seasonTvdbId = episode.getInt(EpisodeQuery.SEASON_ID);
        ServiceUtils.setUpTvdbButton(getShowId(), seasonTvdbId, episodeTvdbId, buttonTvdb, TAG);

        // trakt button
        ServiceUtils.setUpTraktButton(mCurrentEpisodeTvdbId, buttonTrakt, TAG);

        // trakt shouts button
        final String episodeTitle = episode.getString(EpisodeQuery.TITLE);
        buttonComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()) {
                    Intent i = new Intent(getActivity(), TraktCommentsActivity.class);
                    i.putExtras(TraktCommentsActivity.createInitBundleEpisode(episodeTitle,
                            mCurrentEpisodeTvdbId
                    ));
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
        loadTraktRatings();
    }

    private void loadTraktRatings() {
        if (mCurrentEpisodeCursor != null && mCurrentEpisodeCursor.moveToFirst()
                && (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED)) {
            int episodeTvdbId = mCurrentEpisodeCursor.getInt(EpisodeQuery._ID);
            int seasonNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.SEASON);
            int episodeNumber = mCurrentEpisodeCursor.getInt(EpisodeQuery.NUMBER);
            mTraktTask = new TraktRatingsTask(getActivity(), getShowId(), episodeTvdbId,
                    seasonNumber, episodeNumber);
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
        ServiceUtils.loadWithPicasso(getActivity(), TheTVDB.buildScreenshotUrl(imagePath))
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
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(mShowTitle);

        // status
        final TextView statusText = (TextView) getView().findViewById(R.id.showStatus);
        ShowTools.setStatusAndColor(statusText, show.getInt(ShowQuery.SHOW_STATUS));

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

        // next release day and time
        StringBuilder timeAndNetwork = new StringBuilder();
        int releaseTime = show.getInt(ShowQuery.SHOW_RELEASE_TIME);
        if (releaseTime != -1) {
            int weekDay = show.getInt(ShowQuery.SHOW_RELEASE_WEEKDAY);
            Date release = TimeTools.getShowReleaseDateTime(getActivity(),
                    TimeTools.getShowReleaseTime(releaseTime),
                    weekDay,
                    show.getString(ShowQuery.SHOW_RELEASE_TIMEZONE),
                    show.getString(ShowQuery.SHOW_RELEASE_COUNTRY));
            String dayString = TimeTools.formatToLocalDayOrDaily(getActivity(), release, weekDay);
            String timeString = TimeTools.formatToLocalTime(getActivity(), release);
            // "Mon 08:30"
            timeAndNetwork.append(dayString).append(" ").append(timeString);
        }
        // network
        final String network = show.getString(ShowQuery.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            if (timeAndNetwork.length() != 0) {
                timeAndNetwork.append(" ");
            }
            timeAndNetwork.append(getString(R.string.show_on_network, network));
        }
        ((TextView) getView().findViewById(R.id.showmeta)).setText(timeAndNetwork.toString());
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
                    if (!isAdded()) {
                        return;
                    }
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
