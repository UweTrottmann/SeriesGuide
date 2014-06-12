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

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
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
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import de.greenrobot.event.EventBus;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import timber.log.Timber;

/**
 * Displays details about a single episode like summary, ratings and episode image if available.
 */
public class EpisodeDetailsFragment extends Fragment implements ActionsFragmentContract {

    private static final String TAG = "Episode Details";

    private static final String KEY_EPISODE_TVDB_ID = "episodeTvdbId";

    private Handler mHandler = new Handler();
    private TraktSummaryTask mTraktTask;

    protected int mEpisodeFlag;
    protected boolean mCollected;
    protected int mShowTvdbId;
    protected int mSeasonNumber;
    protected int mEpisodeNumber;
    private String mEpisodeTitle;
    private String mShowTitle;

    @InjectView(R.id.scrollViewEpisode) ScrollView mEpisodeScrollView;
    @InjectView(R.id.containerEpisode) View mEpisodeContainer;
    @InjectView(R.id.ratingbar) View mRatingsContainer;
    @InjectView(R.id.containerEpisodeActions) LinearLayout mActionsContainer;

    @InjectView(R.id.containerEpisodeImage) View mImageContainer;
    @InjectView(R.id.imageViewEpisode) ImageView mEpisodeImage;

    @InjectView(R.id.textViewEpisodeTitle) TextView mTitle;
    @InjectView(R.id.textViewEpisodeDescription) TextView mDescription;
    @InjectView(R.id.textViewEpisodeReleaseTime) TextView mReleaseTime;
    @InjectView(R.id.textViewEpisodeReleaseDay) TextView mReleaseDay;
    @InjectView(R.id.textViewEpisodeLastEdit) TextView mLastEdit;
    @InjectView(R.id.labelEpisodeGuestStars) View mLabelGuestStars;
    @InjectView(R.id.textViewEpisodeGuestStars) TextView mGuestStars;
    @InjectView(R.id.textViewEpisodeDirectors) TextView mDirectors;
    @InjectView(R.id.textViewEpisodeWriters) TextView mWriters;
    @InjectView(R.id.labelEpisodeDvd) View mLabelDvd;
    @InjectView(R.id.textViewEpisodeDvd) TextView mDvd;
    @InjectView(R.id.textViewRatingsTvdbValue) TextView mTvdbRating;

    @InjectView(R.id.imageButtonBarCheckin) ImageButton mCheckinButton;
    @InjectView(R.id.imageButtonBarWatched) ImageButton mWatchedButton;
    @InjectView(R.id.imageButtonBarCollected) ImageButton mCollectedButton;
    @InjectView(R.id.imageButtonBarSkip) ImageButton mSkipButton;
    @InjectView(R.id.imageButtonBarMenu) ImageButton mOverflowButton;

    @InjectView(R.id.buttonShowInfoIMDB) View mImdbButton;
    @InjectView(R.id.buttonTVDB) View mTvdbButton;
    @InjectView(R.id.buttonTrakt) View mTraktButton;
    @InjectView(R.id.buttonWebSearch) View mWebSearchButton;
    @InjectView(R.id.buttonShouts) View mCommentsButton;

    /**
     * Data which has to be passed when creating this fragment.
     */
    public interface InitBundle {

        /**
         * Integer extra.
         */
        String EPISODE_TVDBID = "episode_tvdbid";

        /**
         * Boolean extra.
         */
        String IS_IN_MULTIPANE_LAYOUT = "multipane";
    }

    public static EpisodeDetailsFragment newInstance(int episodeId, boolean isInMultiPaneLayout) {
        EpisodeDetailsFragment f = new EpisodeDetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(InitBundle.EPISODE_TVDBID, episodeId);
        args.putBoolean(InitBundle.IS_IN_MULTIPANE_LAYOUT, isInMultiPaneLayout);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_episode, container, false);
        ButterKnife.inject(this, v);

        mEpisodeContainer.setVisibility(View.GONE);

        // web search button unused, is available as extension
        mWebSearchButton.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupViews();

        getLoaderManager().initLoader(EpisodesActivity.EPISODE_LOADER_ID, null,
                mEpisodeDataLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    private void setupViews() {
        if (AndroidUtils.isKitKatOrHigher()) {
            if (getActivity() instanceof EpisodeDetailsActivity) {
                // adapt to translucent system bars
                SystemBarTintManager.SystemBarConfig config
                        = ((EpisodeDetailsActivity) getActivity())
                        .getSystemBarTintManager().getConfig();
                mEpisodeScrollView.setClipToPadding(false);
                mEpisodeScrollView.setPadding(0, 0, 0, config.getPixelInsetBottom());
            }
        }
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

        if (mHandler != null) {
            mHandler.removeCallbacks(mEpisodeActionsRunnable);
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Always cancel the request here, this is safe to call even if the image has been loaded.
        // This ensures that the anonymous callback we have does not prevent the fragment from
        // being garbage collected. It also prevents our callback from getting invoked even after the
        // fragment is destroyed.
        Picasso picasso = ServiceUtils.getExternalPicasso(getActivity());
        if (picasso != null) {
            picasso.cancelRequest(mEpisodeImage);
        }
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        if (mTraktTask != null) {
            mTraktTask.cancel(true);
            mTraktTask = null;
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light;
        // multi-pane layout has non-transparent action bar, adjust icon color
        boolean isInMultipane = getArguments().getBoolean(InitBundle.IS_IN_MULTIPANE_LAYOUT);
        inflater.inflate(isLightTheme && !isInMultipane
                ? R.menu.episodedetails_menu_light : R.menu.episodedetails_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();
        menu.findItem(R.id.menu_manage_lists).setVisible(!isDrawerOpen);
        menu.findItem(R.id.menu_share).setVisible(!isDrawerOpen);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_share) {
            shareEpisode();
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            fireTrackerEvent("Manage lists");
            ListsDialogFragment.showListsDialog(getEpisodeTvdbId(), ListItemTypes.EPISODE,
                    getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getEpisodeTvdbId() {
        return getArguments().getInt(InitBundle.EPISODE_TVDBID);
    }

    /**
     * If episode was watched, flags as unwatched. Otherwise, flags as watched.
     */
    private void onToggleWatched() {
        changeEpisodeFlag(EpisodeTools.isWatched(mEpisodeFlag)
                ? EpisodeFlags.UNWATCHED : EpisodeFlags.WATCHED);
    }

    /**
     * If episode was skipped, flags as unwatched. Otherwise, flags as skipped.
     */
    private void onToggleSkipped() {
        changeEpisodeFlag(EpisodeTools.isSkipped(mEpisodeFlag)
                ? EpisodeFlags.UNWATCHED : EpisodeFlags.SKIPPED);
    }

    private void changeEpisodeFlag(int episodeFlag) {
        mEpisodeFlag = episodeFlag;
        new FlagTask(getActivity(), mShowTvdbId)
                .episodeWatched(getEpisodeTvdbId(), mSeasonNumber, mEpisodeNumber, episodeFlag)
                .execute();
    }

    private void onToggleCollected() {
        mCollected = !mCollected;
        new FlagTask(getActivity(), mShowTvdbId)
                .episodeCollected(getEpisodeTvdbId(), mSeasonNumber, mEpisodeNumber, mCollected)
                .execute();
    }

    @Override
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        if (getEpisodeTvdbId() == event.episodeTvdbId) {
            loadEpisodeActionsDelayed();
        }
    }

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktAction == TraktAction.RATE_EPISODE) {
            loadTraktRatings(false);
        }
    }

    private LoaderManager.LoaderCallbacks<Cursor> mEpisodeDataLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Episodes.buildEpisodeWithShowUri(String
                    .valueOf(getEpisodeTvdbId())), DetailsQuery.PROJECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            populateEpisodeData(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // do nothing (we are never holding onto the cursor
        }
    };

    private void populateEpisodeData(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            // no data to display
            if (mEpisodeContainer != null) {
                mEpisodeContainer.setVisibility(View.GONE);
            }
            return;
        }

        mShowTvdbId = cursor.getInt(DetailsQuery.REF_SHOW_ID);
        mSeasonNumber = cursor.getInt(DetailsQuery.SEASON);
        mEpisodeNumber = cursor.getInt(DetailsQuery.NUMBER);

        // title and description
        mEpisodeTitle = cursor.getString(DetailsQuery.TITLE);
        mTitle.setText(mEpisodeTitle);
        mDescription.setText(cursor.getString(DetailsQuery.OVERVIEW));

        // show title
        mShowTitle = cursor.getString(DetailsQuery.SHOW_TITLE);

        // release time and day
        SpannableStringBuilder timeAndNumbersText = new SpannableStringBuilder();
        final long releaseTime = cursor.getLong(DetailsQuery.FIRST_RELEASE_MS);
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.getEpisodeReleaseTime(getActivity(), releaseTime);
            mReleaseDay.setText(TimeTools.formatToDate(getActivity(), actualRelease));
            // "in 15 mins (Fri)"
            timeAndNumbersText
                    .append(getString(R.string.release_date_and_day,
                            TimeTools.formatToRelativeLocalReleaseTime(getActivity(), actualRelease),
                            TimeTools.formatToLocalReleaseDay(actualRelease))
                            .toUpperCase(Locale.getDefault()));
            timeAndNumbersText.append("  ");
        } else {
            mReleaseDay.setText(R.string.unknown);
        }
        // absolute number (e.g. relevant for Anime): "ABSOLUTE 142"
        int numberStartIndex = timeAndNumbersText.length();
        int absoluteNumber = cursor.getInt(DetailsQuery.ABSOLUTE_NUMBER);
        if (absoluteNumber > 0) {
            timeAndNumbersText
                    .append(getString(R.string.episode_number_absolute))
                    .append(" ")
                    .append(String.valueOf(absoluteNumber));
            // de-emphasize number
            timeAndNumbersText.setSpan(new TextAppearanceSpan(getActivity(),
                            R.style.TextAppearance_Small_Dim), numberStartIndex,
                    timeAndNumbersText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        mReleaseTime.setText(timeAndNumbersText);

        // guest stars
        Utils.setLabelValueOrHide(mLabelGuestStars, mGuestStars,
                Utils.splitAndKitTVDBStrings(cursor.getString(DetailsQuery.GUESTSTARS))
        );
        // DVD episode number
        Utils.setLabelValueOrHide(mLabelDvd, mDvd, cursor.getDouble(DetailsQuery.DVDNUMBER));
        // directors
        Utils.setValueOrPlaceholder(mDirectors, Utils.splitAndKitTVDBStrings(cursor
                .getString(DetailsQuery.DIRECTORS)));
        // writers
        Utils.setValueOrPlaceholder(mWriters, Utils.splitAndKitTVDBStrings(cursor
                .getString(DetailsQuery.WRITERS)));

        // last TVDb edit date
        long lastEditSeconds = cursor.getLong(DetailsQuery.LASTEDIT);
        if (lastEditSeconds > 0) {
            mLastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditSeconds * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            mLastEdit.setText(R.string.unknown);
        }

        // ratings
        mRatingsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateOnTrakt();
            }
        });
        mRatingsContainer.setFocusable(true);
        CheatSheet.setup(mRatingsContainer, R.string.menu_rate_episode);
        // TVDb rating
        String tvdbRating = cursor.getString(DetailsQuery.RATING);
        if (!TextUtils.isEmpty(tvdbRating)) {
            mTvdbRating.setText(tvdbRating);
        }
        // trakt ratings
        loadTraktRatings(true);

        // episode image
        final String imagePath = cursor.getString(DetailsQuery.IMAGE);
        mImageContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fullscreen = new Intent(getActivity(), FullscreenImageActivity.class);
                fullscreen.putExtra(FullscreenImageActivity.InitBundle.IMAGE_PATH, imagePath);
                ActivityCompat.startActivity(getActivity(), fullscreen,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
            }
        });
        loadImage(imagePath);

        // check in button
        final int episodeTvdbId = cursor.getInt(DetailsQuery._ID);
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // display a check-in dialog
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(),
                        episodeTvdbId);
                f.show(getFragmentManager(), "checkin-dialog");
                fireTrackerEvent("Check-In");
            }
        });
        CheatSheet.setup(mCheckinButton);

        // watched button
        mEpisodeFlag = cursor.getInt(DetailsQuery.WATCHED);
        boolean isWatched = EpisodeTools.isWatched(mEpisodeFlag);
        mWatchedButton.setImageResource(isWatched ? R.drawable.ic_ticked
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableWatch));
        mWatchedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleWatched();
                fireTrackerEvent("Toggle watched");
            }
        });
        CheatSheet.setup(mWatchedButton, isWatched ? R.string.unmark_episode
                : R.string.mark_episode);

        // collected button
        mCollected = cursor.getInt(DetailsQuery.COLLECTED) == 1;
        mCollectedButton.setImageResource(mCollected ? R.drawable.ic_collected
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableCollect));
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleCollected();
                fireTrackerEvent("Toggle collected");
            }
        });
        CheatSheet.setup(mCollectedButton, mCollected
                ? R.string.action_collection_remove : R.string.action_collection_add);

        // skip button
        boolean isSkipped = EpisodeTools.isSkipped(mEpisodeFlag);
        if (isWatched) {
            // if watched do not allow skipping
            mSkipButton.setVisibility(View.GONE);
        } else {
            mSkipButton.setVisibility(View.VISIBLE);
            mSkipButton.setImageResource(isSkipped
                    ? R.drawable.ic_action_playback_next_highlight
                    : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableSkip));
            mSkipButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToggleSkipped();
                    fireTrackerEvent("Toggle skipped");
                }
            });
            CheatSheet.setup(mSkipButton,
                    isSkipped ? R.string.action_dont_skip : R.string.action_skip);
        }

        // menu button
        final int showRunTime = cursor.getInt(DetailsQuery.SHOW_RUNTIME);
        mOverflowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenuInflater()
                        .inflate(R.menu.episode_popup_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new OverflowItemClickListener(mShowTitle,
                        Utils.getNextEpisodeString(v.getContext(), mSeasonNumber, mEpisodeNumber,
                                mEpisodeTitle), releaseTime, showRunTime
                ));
                popupMenu.show();
            }
        });

        // service buttons
        ServiceUtils.setUpTraktButton(mShowTvdbId, mSeasonNumber, mEpisodeNumber, mTraktButton,
                TAG);
        // IMDb
        String imdbId = cursor.getString(DetailsQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId)) {
            // fall back to show IMDb id
            imdbId = cursor.getString(DetailsQuery.SHOW_IMDBID);
        }
        ServiceUtils.setUpImdbButton(imdbId, mImdbButton, TAG, getActivity());
        // TVDb
        final int seasonTvdbId = cursor.getInt(DetailsQuery.REF_SEASON_ID);
        ServiceUtils.setUpTvdbButton(mShowTvdbId, seasonTvdbId, getEpisodeTvdbId(), mTvdbButton,
                TAG);
        // trakt comments
        mCommentsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), TraktShoutsActivity.class);
                intent.putExtras(TraktShoutsActivity.createInitBundleEpisode(mShowTvdbId,
                        mSeasonNumber, mEpisodeNumber, mEpisodeTitle));
                startActivity(intent);
                fireTrackerEvent("Comments");
            }
        });

        mEpisodeContainer.setVisibility(View.VISIBLE);
    }

    private class OverflowItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private String mShowTitle;
        private String mEpisodeTitleAndNumber;
        private long mEpisodeReleaseTime;
        private int mShowRunTime;

        public OverflowItemClickListener(String showTitle, String episodeTitleAndNumber,
                long episodeReleaseTime, int showRunTime) {
            mShowTitle = showTitle;
            mEpisodeTitleAndNumber = episodeTitleAndNumber;
            mEpisodeReleaseTime = episodeReleaseTime;
            mShowRunTime = showRunTime;
        }

        @Override
        public boolean onMenuItemClick(android.view.MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_action_episode_calendar:
                    ShareUtils.onAddCalendarEvent(getActivity(), mShowTitle,
                            mEpisodeTitleAndNumber, mEpisodeReleaseTime, mShowRunTime);
                    fireTrackerEvent("Add to calendar");
                    return true;
            }
            return false;
        }
    }

    private void loadTraktRatings(boolean isUseCachedValues) {
        if (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED) {
            mTraktTask = new TraktSummaryTask(getActivity(), mRatingsContainer,
                    isUseCachedValues).episode(mShowTvdbId, getEpisodeTvdbId(), mSeasonNumber,
                    mEpisodeNumber);
            AndroidUtils.executeAsyncTask(mTraktTask);
        }
    }

    private void rateOnTrakt() {
        TraktTools.rateEpisode(getActivity(), getFragmentManager(), mShowTvdbId, mSeasonNumber,
                mEpisodeNumber);
        fireTrackerEvent("Rate (trakt)");
    }

    private void shareEpisode() {
        if (mEpisodeTitle == null || mShowTitle == null) {
            return;
        }
        ShareUtils.shareEpisode(getActivity(), mShowTvdbId, mSeasonNumber, mEpisodeNumber,
                mShowTitle, mEpisodeTitle);

        fireTrackerEvent("Share");
    }

    private void loadImage(String imagePath) {
        // immediately hide container if there is no image
        if (TextUtils.isEmpty(imagePath)) {
            mImageContainer.setVisibility(View.GONE);
            return;
        }

        // try loading image
        mImageContainer.setVisibility(View.VISIBLE);
        Picasso picasso = ServiceUtils.getExternalPicasso(getActivity());
        if (picasso == null) {
            return;
        }
        picasso.load(TheTVDB.buildScreenshotUrl(imagePath))
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
                        Timber.e("onLoadFinished: did not receive valid actions for "
                                + getEpisodeTvdbId());
                    } else {
                        Timber.d("onLoadFinished: received " + data.size() + " actions for "
                                + getEpisodeTvdbId());
                    }
                    EpisodeActionsHelper.populateEpisodeActions(getActivity().getLayoutInflater(),
                            mActionsContainer, data);
                }

                @Override
                public void onLoaderReset(Loader<List<Action>> loader) {
                    // do nothing, we are not holding onto the actions list
                }
            };

    public void loadEpisodeActions() {
        Bundle args = new Bundle();
        args.putInt(KEY_EPISODE_TVDB_ID, getEpisodeTvdbId());
        getLoaderManager().restartLoader(EpisodesActivity.ACTIONS_LOADER_ID, args,
                mEpisodeActionsLoaderCallbacks);
    }

    Runnable mEpisodeActionsRunnable = new Runnable() {
        @Override
        public void run() {
            loadEpisodeActions();
        }
    };

    public void loadEpisodeActionsDelayed() {
        mHandler.removeCallbacks(mEpisodeActionsRunnable);
        mHandler.postDelayed(mEpisodeActionsRunnable,
                ActionsFragmentContract.ACTION_LOADER_DELAY_MILLIS);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    interface DetailsQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Shows.REF_SHOW_ID, Episodes.OVERVIEW,
                Episodes.NUMBER, Episodes.SEASON, Episodes.WATCHED, Episodes.FIRSTAIREDMS,
                Episodes.DIRECTORS, Episodes.GUESTSTARS, Episodes.WRITERS,
                Tables.EPISODES + "." + Episodes.RATING, Episodes.IMAGE, Episodes.DVDNUMBER,
                Episodes.TITLE, Shows.TITLE, Shows.IMDBID, Shows.RUNTIME, Shows.POSTER,
                Seasons.REF_SEASON_ID, Episodes.COLLECTED, Episodes.IMDBID, Episodes.LAST_EDITED,
                Episodes.ABSOLUTE_NUMBER
        };

        int _ID = 0;

        int REF_SHOW_ID = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int FIRST_RELEASE_MS = 6;

        int DIRECTORS = 7;

        int GUESTSTARS = 8;

        int WRITERS = 9;

        int RATING = 10;

        int IMAGE = 11;

        int DVDNUMBER = 12;

        int TITLE = 13;

        int SHOW_TITLE = 14;

        int SHOW_IMDBID = 15;

        int SHOW_RUNTIME = 16;

        int SHOW_POSTER = 17;

        int REF_SEASON_ID = 18;

        int COLLECTED = 19;

        int IMDBID = 20;

        int LASTEDIT = 21;

        int ABSOLUTE_NUMBER = 22;
    }
}
