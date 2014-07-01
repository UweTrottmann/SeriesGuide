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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.loaders.ShowLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import de.greenrobot.event.EventBus;
import java.util.Date;

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

    private Series mShow;

    private TraktSummaryTask mTraktTask;

    private View mCastView;
    private LinearLayout mCastContainer;
    private View mCrewView;
    private LinearLayout mCrewContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show, container, false);

        mCastView = v.findViewById(R.id.containerShowCast);
        TextView castHeader = ButterKnife.findById(mCastView, R.id.textViewPeopleHeader);
        castHeader.setText(R.string.movie_cast);
        mCastContainer = ButterKnife.findById(mCastView, R.id.containerPeople);

        mCrewView = v.findViewById(R.id.containerShowCrew);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.show_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();
        menu.findItem(R.id.menu_show_manage_lists).setVisible(!isDrawerOpen);
        menu.findItem(R.id.menu_show_share).setVisible(!isDrawerOpen);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_show_manage_lists) {
            ManageListsDialogFragment.showListsDialog(getShowTvdbId(), ListItemTypes.SHOW,
                    getFragmentManager());
            return true;
        } else if (itemId == R.id.menu_show_share) {
            shareShow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktAction == TraktAction.RATE_SHOW) {
            onLoadTraktRatings(false);
        }
    }

    private LoaderCallbacks<Series> mShowLoaderCallbacks = new LoaderCallbacks<Series>() {
        @Override
        public Loader<Series> onCreateLoader(int id, Bundle args) {
            return new ShowLoader(getActivity(), getShowTvdbId());
        }

        @Override
        public void onLoadFinished(Loader<Series> loader, Series data) {
            if (data != null) {
                mShow = data;
            }
            if (isAdded()) {
                onPopulateShowData();
            }
        }

        @Override
        public void onLoaderReset(Loader<Series> loader) {
            // do nothing
        }
    };

    private void onPopulateShowData() {
        if (mShow == null) {
            return;
        }

        // status
        TextView status = (TextView) getView().findViewById(R.id.textViewShowStatus);
        if (mShow.getStatus() == 1) {
            status.setTextColor(getResources().getColor(Utils.resolveAttributeToResourceId(
                    getActivity().getTheme(), R.attr.textColorSgGreen)));
            status.setText(getString(R.string.show_isalive));
        } else if (mShow.getStatus() == 0) {
            status.setTextColor(Color.GRAY);
            status.setText(getString(R.string.show_isnotalive));
        }

        // release time
        TextView releaseTime = (TextView) getView().findViewById(R.id.textViewShowReleaseTime);
        if (!TextUtils.isEmpty(mShow.getAirsDayOfWeek()) && mShow.getAirsTime() != -1) {
            String[] values = TimeTools
                    .formatToShowReleaseTimeAndDay(getActivity(), mShow.getAirsTime(),
                            mShow.getCountry(), mShow.getAirsDayOfWeek());
            releaseTime.setText(values[1] + " " + values[0]);
        } else {
            releaseTime.setText(null);
        }

        // runtime
        TextView runtime = (TextView) getView().findViewById(R.id.textViewShowRuntime);
        runtime.setText(getString(R.string.runtime_minutes, mShow.getRuntime()));

        // network
        TextView network = (TextView) getView().findViewById(R.id.textViewShowNetwork);
        network.setText(TextUtils.isEmpty(mShow.getNetwork()) ? null : mShow.getNetwork());

        // overview
        TextView overview = (TextView) getView().findViewById(R.id.textViewShowOverview);
        overview.setText(TextUtils.isEmpty(mShow.getOverview()) ? null : mShow.getOverview());

        // country for release times (or assumed one)
        // show "United States" if country is not supported
        TextView releaseCountry = (TextView) getView()
                .findViewById(R.id.textViewShowReleaseCountry);
        releaseCountry.setText(TimeTools.isUnsupportedCountryOrUs(mShow.getCountry())
                ? TimeTools.UNITED_STATES : mShow.getCountry());

        // first release: use the same parser as for episodes, because we have an exact date
        long actualRelease = TimeTools.parseEpisodeReleaseTime(mShow.getFirstAired(),
                mShow.getAirsTime(), mShow.getCountry());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewShowFirstAirdate),
                TimeTools.formatToDate(getActivity(), new Date(actualRelease)));

        // Others
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewShowContentRating),
                mShow.getContentRating());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewShowGenres),
                Utils.splitAndKitTVDBStrings(mShow.getGenres()));

        // TVDb rating
        String ratingText = mShow.getRating();
        if (ratingText != null && ratingText.length() != 0) {
            TextView rating = (TextView) getView().findViewById(R.id.textViewRatingsTvdbValue);
            rating.setText(ratingText);
        }
        View ratings = getView().findViewById(R.id.ratingbar);
        ratings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRateOnTrakt();
            }
        });
        ratings.setFocusable(true);
        CheatSheet.setup(ratings, R.string.menu_rate_show);

        // Last edit date
        TextView lastEdit = (TextView) getView().findViewById(R.id.textViewShowLastEdit);
        long lastEditRaw = mShow.getLastEdit();
        if (lastEditRaw > 0) {
            lastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            lastEdit.setText(R.string.unknown);
        }

        // IMDb button
        View imdbButton = getView().findViewById(R.id.buttonShowInfoIMDB);
        final String imdbId = mShow.getImdbId();
        ServiceUtils.setUpImdbButton(imdbId, imdbButton, TAG, getActivity());

        // TVDb button
        View tvdbButton = getView().findViewById(R.id.buttonTVDB);
        ServiceUtils.setUpTvdbButton(getShowTvdbId(), tvdbButton, TAG);

        // trakt button
        ServiceUtils.setUpTraktButton(getShowTvdbId(), getView().findViewById(R.id.buttonTrakt),
                TAG);

        // Web search button
        View webSearch = getView().findViewById(R.id.buttonWebSearch);
        ServiceUtils.setUpWebSearchButton(mShow.getTitle(), webSearch, TAG);

        // Shout button
        getView().findViewById(R.id.buttonShouts).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundleShow(mShow.getTitle(),
                        getShowTvdbId()));
                startActivity(i);
                fireTrackerEvent("Shouts");
            }
        });

        // Poster
        final View posterContainer = getView().findViewById(R.id.containerShowPoster);
        final ImageView posterView = (ImageView) posterContainer
                .findViewById(R.id.imageViewShowPoster);
        final String posterPath = mShow.getPoster();
        Utils.loadPoster(getActivity(), posterView, posterPath);
        posterContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fullscreen = new Intent(getActivity(), FullscreenImageActivity.class);
                fullscreen.putExtra(FullscreenImageActivity.InitBundle.IMAGE_PATH,
                        TheTVDB.buildScreenshotUrl(posterPath));
                ActivityCompat.startActivity(getActivity(), fullscreen,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
            }
        });

        // background poster
        ImageView background = (ImageView) getView()
                .findViewById(R.id.imageViewShowPosterBackground);
        Utils.loadPosterBackground(getActivity(), background, posterPath);

        // trakt ratings
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

    private void populateCredits(final Credits data) {
        if (data == null) {
            mCastView.setVisibility(View.GONE);
            mCrewView.setVisibility(View.GONE);
            return;
        }

        if (data.cast == null || data.cast.size() == 0) {
            mCastView.setVisibility(View.GONE);
        } else {
            mCastView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateCast(getActivity(), getActivity().getLayoutInflater(),
                    mCastContainer, data.cast, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(v.getContext(), PeopleActivity.class)
                                    .putExtra(PeopleActivity.InitBundle.PEOPLE_TYPE,
                                            PeopleActivity.PeopleType.CAST.toString())
                                    .putExtra(PeopleActivity.InitBundle.MEDIA_TYPE,
                                            PeopleActivity.MediaType.SHOW.toString())
                                    .putExtra(PeopleActivity.InitBundle.TMDB_ID, data.id));
                        }
                    }
            );
        }

        if (data.crew == null || data.crew.size() == 0) {
            mCrewView.setVisibility(View.GONE);
        } else {
            mCrewView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateCrew(getActivity(), getActivity().getLayoutInflater(),
                    mCrewContainer, data.crew, new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(v.getContext(), PeopleActivity.class)
                                    .putExtra(PeopleActivity.InitBundle.PEOPLE_TYPE,
                                            PeopleActivity.PeopleType.CREW.toString())
                                    .putExtra(PeopleActivity.InitBundle.MEDIA_TYPE,
                                            PeopleActivity.MediaType.SHOW.toString())
                                    .putExtra(PeopleActivity.InitBundle.TMDB_ID, data.id));
                        }
                    }
            );
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
        if (mShow != null
                && (mTraktTask == null || mTraktTask.getStatus() == AsyncTask.Status.FINISHED)) {
            mTraktTask = new TraktSummaryTask(getActivity(), getView().findViewById(
                    R.id.ratingbar), isUseCachedValues).show(getShowTvdbId());
            AndroidUtils.executeOnPool(mTraktTask);
        }
    }

    private void shareShow() {
        if (mShow != null) {
            ShareUtils.shareShow(getActivity(), getShowTvdbId(), mShow.getTitle());
            fireTrackerEvent("Share");
        }
    }
}
