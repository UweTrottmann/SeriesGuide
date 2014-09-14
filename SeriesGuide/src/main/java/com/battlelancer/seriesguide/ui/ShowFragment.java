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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShortcutUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import de.greenrobot.event.EventBus;
import java.util.Date;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;

/**
 *
 */
public class ShowFragment extends Fragment {

    public interface InitBundle {

        String SHOW_TVDBID = "tvdbid";
    }

    private static final String TAG = "Show Info";

    public static ShowFragment newInstance(int showTvdbId) {
        ShowFragment f = new ShowFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    private Cursor mShowCursor;

    private TraktSummaryTask mTraktTask;

    @InjectView(R.id.textViewShowStatus) TextView mTextViewStatus;
    @InjectView(R.id.textViewShowReleaseTime) TextView mTextViewReleaseTime;
    @InjectView(R.id.textViewShowRuntime) TextView mTextViewRuntime;
    @InjectView(R.id.textViewShowNetwork) TextView mTextViewNetwork;
    @InjectView(R.id.textViewShowOverview) TextView mTextViewOverview;
    @InjectView(R.id.textViewShowReleaseCountry) TextView mTextViewReleaseCountry;
    @InjectView(R.id.textViewShowFirstAirdate) TextView mTextViewFirstRelease;
    @InjectView(R.id.textViewShowContentRating) TextView mTextViewContentRating;
    @InjectView(R.id.textViewShowGenres) TextView mTextViewGenres;
    @InjectView(R.id.textViewRatingsTvdbValue) TextView mTextViewTvdbRating;
    @InjectView(R.id.textViewShowLastEdit) TextView mTextViewLastEdit;

    @InjectView(R.id.buttonShowInfoIMDB) View mButtonImdb;
    @InjectView(R.id.buttonShowFavorite) Button mButtonFavorite;
    @InjectView(R.id.buttonShowShare) Button mButtonShare;
    @InjectView(R.id.buttonShowShortcut) Button mButtonShortcut;
    @InjectView(R.id.ratingbar) View mButtonRate;
    @InjectView(R.id.buttonTVDB) View mButtonTvdb;
    @InjectView(R.id.buttonTrakt) View mButtonTrakt;
    @InjectView(R.id.buttonWebSearch) View mButtonWebSearch;
    @InjectView(R.id.buttonShouts) View mButtonComments;

    @InjectView(R.id.containerShowCast) View mCastView;
    private LinearLayout mCastContainer;
    @InjectView(R.id.containerShowCrew) View mCrewView;
    private LinearLayout mCrewContainer;

    private String mShowTitle;
    private String mShowPoster;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show, container, false);
        ButterKnife.inject(this, v);

        // share button
        mButtonShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareShow();
            }
        });
        CheatSheet.setup(mButtonShare);

        // shortcut button
        mButtonShortcut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createShortcut();
            }
        });
        CheatSheet.setup(mButtonShortcut);

        // rate button
        mButtonRate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRateOnTrakt();
            }
        });
        CheatSheet.setup(mButtonRate, R.string.action_rate);

        TextView castHeader = ButterKnife.findById(mCastView, R.id.textViewPeopleHeader);
        castHeader.setText(R.string.movie_cast);
        mCastContainer = ButterKnife.findById(mCastView, R.id.containerPeople);

        TextView crewHeader = ButterKnife.findById(mCrewView, R.id.textViewPeopleHeader);
        crewHeader.setText(R.string.movie_crew);
        mCrewContainer = ButterKnife.findById(mCrewView, R.id.containerPeople);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(OverviewActivity.SHOW_LOADER_ID, null, mShowLoaderCallbacks);
        getLoaderManager().initLoader(OverviewActivity.SHOW_CREDITS_LOADER_ID, null,
                mCreditsLoaderCallbacks);

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
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.show_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_show_manage_lists) {
            ManageListsDialogFragment.showListsDialog(getShowTvdbId(), ListItemTypes.SHOW,
                    getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktAction == TraktAction.RATE_SHOW) {
            onLoadTraktRatings(false);
        }
    }

    interface ShowQuery {

        String[] PROJECTION = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.AIRSTIME,
                Shows.AIRSDAYOFWEEK,
                Shows.NETWORK,
                Shows.POSTER,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE,
                Shows.RELEASE_COUNTRY,
                Shows.OVERVIEW,
                Shows.FIRSTAIRED,
                Shows.CONTENTRATING,
                Shows.GENRES,
                Shows.RATING,
                Shows.LASTEDIT
        };

        int TITLE = 1;
        int STATUS = 2;
        int RELEASE_TIME_MS = 3;
        int RELEASE_DAY = 4;
        int NETWORK = 5;
        int POSTER = 6;
        int IMDBID = 7;
        int RUNTIME = 8;
        int IS_FAVORITE = 9;
        int RELEASE_COUNTRY = 10;
        int OVERVIEW = 11;
        int FIRST_RELEASE = 12;
        int CONTENT_RATING = 13;
        int GENRES = 14;
        int TVDB_RATING = 15;
        int LAST_EDIT_MS = 16;
    }

    private LoaderCallbacks<Cursor> mShowLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Shows.buildShowUri(getShowTvdbId()),
                    ShowQuery.PROJECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.moveToFirst()) {
                mShowCursor = data;
                if (isAdded()) {
                    populateShow();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // do nothing, prefer stale data
        }
    };

    private void populateShow() {
        if (mShowCursor == null) {
            return;
        }

        // title
        mShowTitle = mShowCursor.getString(ShowQuery.TITLE);
        mShowPoster = mShowCursor.getString(ShowQuery.POSTER);

        // status
        if (mShowCursor.getInt(ShowQuery.STATUS) == 1) {
            mTextViewStatus.setTextColor(getResources().getColor(
                    Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.textColorSgGreen)));
            mTextViewStatus.setText(getString(R.string.show_isalive));
        } else {
            mTextViewStatus.setTextColor(Color.GRAY);
            mTextViewStatus.setText(getString(R.string.show_isnotalive));
        }

        // release time
        String releaseDay = mShowCursor.getString(ShowQuery.RELEASE_DAY);
        long releaseTime = mShowCursor.getLong(ShowQuery.RELEASE_TIME_MS);
        String releaseCountry = mShowCursor.getString(ShowQuery.RELEASE_COUNTRY);
        if (!TextUtils.isEmpty(releaseDay)) {
            String[] values = TimeTools.formatToShowReleaseTimeAndDay(getActivity(), releaseTime,
                    releaseCountry, releaseDay);
            mTextViewReleaseTime.setText(values[1] + " " + values[0]);
        } else {
            mTextViewReleaseTime.setText(null);
        }

        // runtime
        mTextViewRuntime.setText(
                getString(R.string.runtime_minutes, mShowCursor.getInt(ShowQuery.RUNTIME)));

        // network
        mTextViewNetwork.setText(mShowCursor.getString(ShowQuery.NETWORK));

        // favorite button
        final boolean isFavorite = mShowCursor.getInt(ShowQuery.IS_FAVORITE) == 1;
        mButtonFavorite.setEnabled(true);
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mButtonFavorite, 0,
                Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        isFavorite ? R.attr.drawableStar : R.attr.drawableStar0),
                0, 0);
        mButtonFavorite.setText(
                isFavorite ? R.string.context_unfavorite : R.string.context_favorite);
        CheatSheet.setup(mButtonFavorite,
                isFavorite ? R.string.context_unfavorite : R.string.context_favorite);
        mButtonFavorite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable until action is complete
                v.setEnabled(false);
                ShowTools.get(v.getContext()).storeIsFavorite(getShowTvdbId(), !isFavorite);
            }
        });

        // overview
        mTextViewOverview.setText(mShowCursor.getString(ShowQuery.OVERVIEW));

        // country for release times (or assumed one)
        // show "United States" if country is not supported
        mTextViewReleaseCountry.setText(TimeTools.isUnsupportedCountryOrUs(releaseCountry)
                ? TimeTools.UNITED_STATES : releaseCountry);

        // original release
        String firstRelease = mShowCursor.getString(ShowQuery.FIRST_RELEASE);
        Utils.setValueOrPlaceholder(mTextViewFirstRelease,
                TimeTools.getShowReleaseYear(firstRelease, releaseTime, releaseCountry));

        // content rating
        Utils.setValueOrPlaceholder(mTextViewContentRating,
                mShowCursor.getString(ShowQuery.CONTENT_RATING));
        // genres
        Utils.setValueOrPlaceholder(mTextViewGenres,
                Utils.splitAndKitTVDBStrings(mShowCursor.getString(ShowQuery.GENRES)));

        // TVDb rating
        String tvdbRating = mShowCursor.getString(ShowQuery.TVDB_RATING);
        if (!TextUtils.isEmpty(tvdbRating)) {
            mTextViewTvdbRating.setText(tvdbRating);
        }

        // last edit
        long lastEditRaw = mShowCursor.getLong(ShowQuery.LAST_EDIT_MS);
        if (lastEditRaw > 0) {
            mTextViewLastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            mTextViewLastEdit.setText(R.string.unknown);
        }

        // IMDb button
        String imdbId = mShowCursor.getString(ShowQuery.IMDBID);
        ServiceUtils.setUpImdbButton(imdbId, mButtonImdb, TAG, getActivity());

        // TVDb button
        ServiceUtils.setUpTvdbButton(getShowTvdbId(), mButtonTvdb, TAG);

        // trakt button
        ServiceUtils.setUpTraktButton(getShowTvdbId(), mButtonTrakt, TAG);

        // web search button
        ServiceUtils.setUpWebSearchButton(mShowTitle, mButtonWebSearch, TAG);

        // shout button
        mButtonComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundleShow(mShowTitle,
                        getShowTvdbId()));
                ActivityCompat.startActivity(getActivity(), i,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
                fireTrackerEvent("Shouts");
            }
        });

        // poster, full screen poster button
        final View posterContainer = getView().findViewById(R.id.containerShowPoster);
        final ImageView posterView = (ImageView) posterContainer
                .findViewById(R.id.imageViewShowPoster);
        Utils.loadPoster(getActivity(), posterView, mShowPoster);
        posterContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fullscreen = new Intent(getActivity(), FullscreenImageActivity.class);
                fullscreen.putExtra(FullscreenImageActivity.InitBundle.IMAGE_PATH,
                        TheTVDB.buildScreenshotUrl(mShowPoster));
                ActivityCompat.startActivity(getActivity(), fullscreen,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
            }
        });

        // background
        ImageView background = (ImageView) getView().findViewById(
                R.id.imageViewShowPosterBackground);
        Utils.loadPosterBackground(getActivity(), background, mShowPoster);

        onLoadTraktRatings(true);
    }

    private LoaderCallbacks<Credits> mCreditsLoaderCallbacks = new LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int id, Bundle args) {
            return new ShowCreditsLoader(getActivity(), getShowTvdbId(), true);
        }

        @Override
        public void onLoadFinished(Loader<Credits> loader, Credits data) {
            if (isAdded()) {
                populateCredits(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> loader) {

        }
    };

    private void populateCredits(final Credits credits) {
        if (credits == null) {
            mCastView.setVisibility(View.GONE);
            mCrewView.setVisibility(View.GONE);
            return;
        }

        if (credits.cast == null || credits.cast.size() == 0) {
            mCastView.setVisibility(View.GONE);
        } else {
            mCastView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateShowCast(getActivity(), getActivity().getLayoutInflater(),
                    mCastContainer, credits);
        }

        if (credits.crew == null || credits.crew.size() == 0) {
            mCrewView.setVisibility(View.GONE);
        } else {
            mCrewView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateShowCrew(getActivity(), getActivity().getLayoutInflater(),
                    mCrewContainer, credits);
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private int getShowTvdbId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onRateOnTrakt() {
        if (TraktCredentials.ensureCredentials(getActivity())) {
            TraktRateDialogFragment rateShow = TraktRateDialogFragment.newInstanceShow(
                    getShowTvdbId());
            rateShow.show(getFragmentManager(), "traktratedialog");
        }
        fireTrackerEvent("Rate (trakt)");
    }

    private void onLoadTraktRatings(boolean isUseCachedValues) {
        if (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED) {
            mTraktTask = new TraktSummaryTask(getActivity(), getView().findViewById(R.id.ratingbar),
                    isUseCachedValues).show(getShowTvdbId());
            AndroidUtils.executeOnPool(mTraktTask);
        }
    }

    private void createShortcut() {
        if (!Utils.hasAccessToX(getActivity())) {
            Utils.advertiseSubscription(getActivity());
            return;
        }

        if (mShowCursor == null) {
            return;
        }

        // create the shortcut
        ShortcutUtils.createShortcut(getActivity(), mShowTitle, mShowPoster, getShowTvdbId());

        // drop to home screen
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK));

        // Analytics
        fireTrackerEvent("Add to Homescreen");
    }

    private void shareShow() {
        if (mShowCursor != null) {
            ShareUtils.shareShow(getActivity(), getShowTvdbId(), mShowTitle);
            fireTrackerEvent("Share");
        }
    }
}
