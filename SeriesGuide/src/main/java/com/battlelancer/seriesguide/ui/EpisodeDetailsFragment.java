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
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
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
import java.util.Locale;
import timber.log.Timber;

/**
 * Displays details about a single episode like summary, ratings and episode image if available.
 */
public class EpisodeDetailsFragment extends Fragment implements ActionsFragmentContract {

    private static final String TAG = "Episode Details";

    private static final String KEY_EPISODE_TVDB_ID = "episodeTvdbId";

    private Handler mHandler = new Handler();
    private TraktRatingsTask mTraktTask;

    protected int mEpisodeFlag;
    protected boolean mCollected;
    protected int mShowTvdbId;
    protected int mSeasonNumber;
    protected int mEpisodeNumber;
    private String mEpisodeTitle;
    private String mShowTitle;
    private int mShowRunTime;
    private long mEpisodeReleaseTime;

    @Bind(R.id.containerEpisode) View mEpisodeContainer;
    @Bind(R.id.containerRatings) View mRatingsContainer;
    @Bind(R.id.containerEpisodeActions) LinearLayout mActionsContainer;

    @Bind(R.id.containerEpisodeImage) View mImageContainer;
    @Bind(R.id.imageViewEpisode) ImageView mEpisodeImage;

    @Bind(R.id.textViewEpisodeTitle) TextView mTitle;
    @Bind(R.id.textViewEpisodeDescription) TextView mDescription;
    @Bind(R.id.textViewEpisodeReleaseTime) TextView mReleaseTime;
    @Bind(R.id.textViewEpisodeReleaseDay) TextView mReleaseDay;
    @Bind(R.id.textViewEpisodeLastEdit) TextView mLastEdit;
    @Bind(R.id.labelEpisodeGuestStars) View mLabelGuestStars;
    @Bind(R.id.textViewEpisodeGuestStars) TextView mGuestStars;
    @Bind(R.id.textViewEpisodeDirectors) TextView mDirectors;
    @Bind(R.id.textViewEpisodeWriters) TextView mWriters;
    @Bind(R.id.labelEpisodeDvd) View mLabelDvd;
    @Bind(R.id.textViewEpisodeDvd) TextView mDvd;
    @Bind(R.id.textViewRatingsValue) TextView mTextRating;
    @Bind(R.id.textViewRatingsVotes) TextView mTextRatingVotes;
    @Bind(R.id.textViewRatingsUser) TextView mTextUserRating;

    @Bind(R.id.buttonEpisodeCheckin) Button mCheckinButton;
    @Bind(R.id.buttonEpisodeWatched) Button mWatchedButton;
    @Bind(R.id.buttonEpisodeCollected) Button mCollectedButton;
    @Bind(R.id.buttonEpisodeSkip) Button mSkipButton;

    @Bind(R.id.buttonShowInfoIMDB) View mImdbButton;
    @Bind(R.id.buttonTVDB) View mTvdbButton;
    @Bind(R.id.buttonTrakt) View mTraktButton;
    @Bind(R.id.buttonWebSearch) View mWebSearchButton;
    @Bind(R.id.buttonShouts) View mCommentsButton;

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
        ButterKnife.bind(this, v);

        mEpisodeContainer.setVisibility(View.GONE);

        // web search button unused, is available as extension
        mWebSearchButton.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(EpisodesActivity.EPISODE_LOADER_ID, null,
                mEpisodeDataLoaderCallbacks);

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
        ServiceUtils.getPicasso(getActivity()).cancelRequest(mEpisodeImage);
        ButterKnife.unbind(this);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_share) {
            shareEpisode();
            return true;
        } else if (itemId == R.id.menu_manage_lists) {
            ManageListsDialogFragment.showListsDialog(getEpisodeTvdbId(), ListItemTypes.EPISODE,
                    getFragmentManager());
            fireTrackerEvent("Manage lists");
            return true;
        } else if (itemId == R.id.menu_action_episode_calendar) {
            ShareUtils.suggestCalendarEvent(getActivity(), mShowTitle,
                    Utils.getNextEpisodeString(getActivity(), mSeasonNumber, mEpisodeNumber,
                            mEpisodeTitle), mEpisodeReleaseTime, mShowRunTime);
            fireTrackerEvent("Add to calendar");
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
        EpisodeTools.episodeWatched(getActivity(), mShowTvdbId, getEpisodeTvdbId(), mSeasonNumber,
                mEpisodeNumber, episodeFlag);
    }

    private void onToggleCollected() {
        mCollected = !mCollected;
        EpisodeTools.episodeCollected(getActivity(), mShowTvdbId, getEpisodeTvdbId(), mSeasonNumber,
                mEpisodeNumber, mCollected);
    }

    @Override
    public void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event) {
        if (getEpisodeTvdbId() == event.episodeTvdbId) {
            loadEpisodeActionsDelayed();
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
            if (!isAdded()) {
                return;
            }
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

        mShowTvdbId = cursor.getInt(DetailsQuery.SHOW_ID);
        mSeasonNumber = cursor.getInt(DetailsQuery.SEASON);
        mEpisodeNumber = cursor.getInt(DetailsQuery.NUMBER);
        mShowRunTime = cursor.getInt(DetailsQuery.SHOW_RUNTIME);
        mEpisodeReleaseTime = cursor.getLong(DetailsQuery.FIRST_RELEASE_MS);

        // title and description
        mEpisodeTitle = cursor.getString(DetailsQuery.TITLE);
        mTitle.setText(mEpisodeTitle);
        mDescription.setText(cursor.getString(DetailsQuery.OVERVIEW));

        // show title
        mShowTitle = cursor.getString(DetailsQuery.SHOW_TITLE);

        // release time and day
        SpannableStringBuilder timeAndNumbersText = new SpannableStringBuilder();
        if (mEpisodeReleaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(getActivity(),
                    mEpisodeReleaseTime);
            mReleaseDay.setText(TimeTools.formatToLocalDateAndDay(getActivity(), actualRelease));
            // "in 15 mins (Fri)"
            timeAndNumbersText
                    .append(getString(R.string.release_date_and_day,
                            TimeTools.formatToLocalRelativeTime(getActivity(),
                                    actualRelease),
                            TimeTools.formatToLocalDay(actualRelease)
                    )
                            .toUpperCase(Locale.getDefault()));
            timeAndNumbersText.append("  ");
        } else {
            mReleaseDay.setText(R.string.unknown);
        }
        // absolute number (e.g. relevant for Anime): "ABSOLUTE 142"
        int numberStartIndex = timeAndNumbersText.length();
        int absoluteNumber = cursor.getInt(DetailsQuery.NUMBER_ABSOLUTE);
        if (absoluteNumber > 0) {
            timeAndNumbersText
                    .append(getString(R.string.episode_number_absolute))
                    .append(" ")
                    .append(String.valueOf(absoluteNumber));
            // de-emphasize number
            timeAndNumbersText.setSpan(new TextAppearanceSpan(getActivity(),
                            R.style.TextAppearance_Caption_Dim), numberStartIndex,
                    timeAndNumbersText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        mReleaseTime.setText(timeAndNumbersText);

        // guest stars
        Utils.setLabelValueOrHide(mLabelGuestStars, mGuestStars,
                Utils.splitAndKitTVDBStrings(cursor.getString(DetailsQuery.GUESTSTARS))
        );
        // DVD episode number
        Utils.setLabelValueOrHide(mLabelDvd, mDvd, cursor.getDouble(DetailsQuery.NUMBER_DVD));
        // directors
        Utils.setValueOrPlaceholder(mDirectors, Utils.splitAndKitTVDBStrings(cursor
                .getString(DetailsQuery.DIRECTORS)));
        // writers
        Utils.setValueOrPlaceholder(mWriters, Utils.splitAndKitTVDBStrings(cursor
                .getString(DetailsQuery.WRITERS)));

        // last TVDb edit date
        long lastEditSeconds = cursor.getLong(DetailsQuery.LAST_EDITED);
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
                rateEpisode();
            }
        });
        mRatingsContainer.setFocusable(true);
        CheatSheet.setup(mRatingsContainer, R.string.action_rate);

        // trakt rating
        mTextRating.setText(
                TraktTools.buildRatingString(cursor.getDouble(DetailsQuery.RATING_GLOBAL)));
        mTextRatingVotes.setText(TraktTools.buildRatingVotesString(getActivity(),
                cursor.getInt(DetailsQuery.RATING_VOTES)));

        // user rating
        mTextUserRating.setText(TraktTools.buildUserRatingString(getActivity(),
                cursor.getInt(DetailsQuery.RATING_USER)));

        loadTraktRatings();

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
                if (f != null) {
                    f.show(getFragmentManager(), "checkin-dialog");
                    fireTrackerEvent("Check-In");
                }
            }
        });
        CheatSheet.setup(mCheckinButton);

        // watched button
        mEpisodeFlag = cursor.getInt(DetailsQuery.WATCHED);
        boolean isWatched = EpisodeTools.isWatched(mEpisodeFlag);
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mWatchedButton, 0,
                isWatched ? Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableWatched)
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableWatch), 0, 0);
        mWatchedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                onToggleWatched();
                fireTrackerEvent("Toggle watched");
            }
        });
        mWatchedButton.setEnabled(true);
        mWatchedButton.setText(isWatched ? R.string.action_unwatched : R.string.action_watched);
        CheatSheet.setup(mWatchedButton, isWatched ? R.string.action_unwatched
                : R.string.action_watched);

        // collected button
        mCollected = cursor.getInt(DetailsQuery.COLLECTED) == 1;
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mCollectedButton, 0,
                mCollected ? R.drawable.ic_collected
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableCollect), 0, 0);
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                onToggleCollected();
                fireTrackerEvent("Toggle collected");
            }
        });
        mCollectedButton.setEnabled(true);
        mCollectedButton.setText(mCollected
                ? R.string.action_collection_remove : R.string.action_collection_add);
        CheatSheet.setup(mCollectedButton, mCollected
                ? R.string.action_collection_remove : R.string.action_collection_add);

        // skip button
        boolean isSkipped = EpisodeTools.isSkipped(mEpisodeFlag);
        if (isWatched) {
            // if watched do not allow skipping
            mSkipButton.setVisibility(View.INVISIBLE);
        } else {
            mSkipButton.setVisibility(View.VISIBLE);
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mSkipButton, 0,
                    isSkipped
                            ? R.drawable.ic_skipped
                            : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                    R.attr.drawableSkip), 0, 0);
            mSkipButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disable button, will be re-enabled on data reload once action completes
                    v.setEnabled(false);
                    onToggleSkipped();
                    fireTrackerEvent("Toggle skipped");
                }
            });
            mSkipButton.setText(isSkipped ? R.string.action_dont_skip : R.string.action_skip);
            CheatSheet.setup(mSkipButton,
                    isSkipped ? R.string.action_dont_skip : R.string.action_skip);
        }
        mSkipButton.setEnabled(true);

        // service buttons
        ServiceUtils.setUpTraktButton(getEpisodeTvdbId(), mTraktButton, TAG);
        // IMDb
        String imdbId = cursor.getString(DetailsQuery.IMDBID);
        if (TextUtils.isEmpty(imdbId)) {
            // fall back to show IMDb id
            imdbId = cursor.getString(DetailsQuery.SHOW_IMDBID);
        }
        ServiceUtils.setUpImdbButton(imdbId, mImdbButton, TAG);
        // TVDb
        final int seasonTvdbId = cursor.getInt(DetailsQuery.SEASON_ID);
        ServiceUtils.setUpTvdbButton(mShowTvdbId, seasonTvdbId, getEpisodeTvdbId(), mTvdbButton,
                TAG);
        // trakt comments
        mCommentsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), TraktCommentsActivity.class);
                intent.putExtras(TraktCommentsActivity.createInitBundleEpisode(mEpisodeTitle,
                        getEpisodeTvdbId()
                ));
                ActivityCompat.startActivity(getActivity(), intent,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
                fireTrackerEvent("Comments");
            }
        });

        mEpisodeContainer.setVisibility(View.VISIBLE);
    }

    private void loadTraktRatings() {
        if (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED) {
            mTraktTask = new TraktRatingsTask(getActivity(), mShowTvdbId, getEpisodeTvdbId(),
                    mSeasonNumber, mEpisodeNumber);
            AndroidUtils.executeOnPool(mTraktTask);
        }
    }

    private void rateEpisode() {
        EpisodeTools.displayRateDialog(getActivity(), getFragmentManager(), getEpisodeTvdbId());
        fireTrackerEvent("Rate (trakt)");
    }

    private void shareEpisode() {
        if (mEpisodeTitle == null || mShowTitle == null) {
            return;
        }
        ShareUtils.shareEpisode(getActivity(), getEpisodeTvdbId(), mSeasonNumber, mEpisodeNumber,
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
                Tables.EPISODES + "." + Episodes._ID,
                Episodes.NUMBER,
                Episodes.ABSOLUTE_NUMBER,
                Episodes.DVDNUMBER,
                Seasons.REF_SEASON_ID,
                Episodes.SEASON,
                Episodes.IMDBID,
                Episodes.TITLE,
                Episodes.OVERVIEW,
                Episodes.FIRSTAIREDMS,
                Episodes.DIRECTORS,
                Episodes.GUESTSTARS,
                Episodes.WRITERS,
                Episodes.IMAGE,
                Tables.EPISODES + "." + Episodes.RATING_GLOBAL,
                Episodes.RATING_VOTES,
                Episodes.RATING_USER,
                Episodes.WATCHED,
                Episodes.COLLECTED,
                Episodes.LAST_EDITED,
                Shows.REF_SHOW_ID,
                Shows.IMDBID,
                Shows.TITLE,
                Shows.RUNTIME
        };

        int _ID = 0;
        int NUMBER = 1;
        int NUMBER_ABSOLUTE = 2;
        int NUMBER_DVD = 3;
        int SEASON_ID = 4;
        int SEASON = 5;
        int IMDBID = 6;
        int TITLE = 7;
        int OVERVIEW = 8;
        int FIRST_RELEASE_MS = 9;
        int DIRECTORS = 10;
        int GUESTSTARS = 11;
        int WRITERS = 12;
        int IMAGE = 13;
        int RATING_GLOBAL = 14;
        int RATING_VOTES = 15;
        int RATING_USER = 16;
        int WATCHED = 17;
        int COLLECTED = 18;
        int LAST_EDITED = 19;
        int SHOW_ID = 20;
        int SHOW_IMDBID = 21;
        int SHOW_TITLE = 22;
        int SHOW_RUNTIME = 23;
    }
}
