
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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.loaders.ShowLoader;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TraktSummaryTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.greenrobot.event.EventBus;

/**
 *
 */
public class ShowFragment extends SherlockFragment implements LoaderCallbacks<Series> {

    public interface InitBundle {

        String SHOW_TVDBID = "tvdbid";
    }

    private static final String TAG = "Show Info";

    private static final int LOADER_ID = R.layout.fragment_show;

    public static ShowFragment newInstance(int showTvdbId) {
        ShowFragment f = new ShowFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    private Series mShow;

    private TraktSummaryTask mTraktTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(LOADER_ID, null, this);

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
        inflater.inflate(R.menu.showinfo_menu, menu);
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
            ListsDialogFragment.showListsDialog(String.valueOf(getShowTvdbId()),
                    ListItemTypes.SHOW, getFragmentManager());
            return true;
        } else if (itemId == R.id.menu_show_share) {
            onShareShow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Series> onCreateLoader(int loaderId, Bundle args) {
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
    }

    public void onEvent(TraktActionCompleteEvent event) {
        if (event.mTraktTaskArgs.getInt(TraktTask.InitBundle.TRAKTACTION)
                == TraktAction.RATE_SHOW.index) {
            onLoadTraktRatings(false);
        }
    }

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
            String[] values = Utils.parseMillisecondsToTime(mShow.getAirsTime(),
                    mShow.getAirsDayOfWeek(), getActivity());
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

        // first airdate
        long airtime = Utils.buildEpisodeAirtime(mShow.getFirstAired(), mShow.getAirsTime());
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewShowFirstAirdate),
                Utils.formatToDate(airtime, getActivity()));

        // Others
        Utils.setValueOrPlaceholder(getView().findViewById(R.id.textViewShowActors),
                Utils.splitAndKitTVDBStrings(mShow.getActors()));
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

        // Google Play button
        View playButton = getView().findViewById(R.id.buttonGooglePlay);
        ServiceUtils.setUpGooglePlayButton(mShow.getTitle(), playButton, TAG);

        // Amazon button
        View amazonButton = getView().findViewById(R.id.buttonAmazon);
        ServiceUtils.setUpAmazonButton(mShow.getTitle(), amazonButton, TAG);

        // YouTube button
        View youtubeButton = getView().findViewById(R.id.buttonYouTube);
        ServiceUtils.setUpYouTubeButton(mShow.getTitle(), youtubeButton, TAG);

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
        final String imagePath = mShow.getPoster();
        ImageProvider.getInstance(getActivity()).loadImage(posterView, imagePath, false);
        posterContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fullscreen = new Intent(getActivity(), FullscreenImageActivity.class);
                fullscreen.putExtra(FullscreenImageActivity.InitBundle.IMAGE_PATH, imagePath);
                fullscreen
                        .putExtra(FullscreenImageActivity.InitBundle.IMAGE_TITLE, mShow.getTitle());
                ActivityCompat.startActivity(getActivity(), fullscreen,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle());
            }
        });

        // background poster
        ImageView background = (ImageView) getView()
                .findViewById(R.id.imageViewShowPosterBackground);
        Utils.setPosterBackground(background, imagePath, getActivity());

        // trakt ratings
        onLoadTraktRatings(true);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private int getShowTvdbId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onRateOnTrakt() {
        if (TraktCredentials.ensureCredentials(getActivity())) {
            TraktRateDialogFragment rateShow = TraktRateDialogFragment.newInstance(getShowTvdbId());
            rateShow.show(getFragmentManager(), "traktratedialog");
        }
        fireTrackerEvent("Rate (trakt)");
    }

    private void onLoadTraktRatings(boolean isUseCachedValues) {
        if (mShow != null
                && (mTraktTask == null || mTraktTask.getStatus() != AsyncTask.Status.RUNNING)) {
            mTraktTask = new TraktSummaryTask(getActivity(), getView().findViewById(
                    R.id.ratingbar), isUseCachedValues).show(getShowTvdbId());
            AndroidUtils.executeAsyncTask(mTraktTask);
        }
    }

    private void onShareShow() {
        if (mShow != null) {
            // Share intent
            IntentBuilder ib = ShareCompat.IntentBuilder
                    .from(getActivity())
                    .setChooserTitle(R.string.share_show)
                    .setText(
                            getString(R.string.share_checkout) + " \"" + mShow.getTitle()
                                    + "\" " + ServiceUtils.IMDB_TITLE_URL + mShow.getImdbId())
                    .setType("text/plain");
            ib.startChooser();
        }
    }
}
