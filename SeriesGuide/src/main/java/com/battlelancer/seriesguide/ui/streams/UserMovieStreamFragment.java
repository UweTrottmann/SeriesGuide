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

package com.battlelancer.seriesguide.ui.streams;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MovieHistoryAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.HistoryActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Displays a stream of movies the user has recently watched on trakt.
 */
public class UserMovieStreamFragment extends StreamFragment {

    private MovieHistoryAdapter mAdapter;

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new MovieHistoryAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not respond if we get a header position by accident
        if (position < 0) {
            return;
        }

        HistoryEntry item = mAdapter.getItem(position);
        if (item == null) {
            return;
        }

        // display movie details
        if (item.movie == null || item.movie.ids == null) {
            return;
        }
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, item.movie.ids.tmdb);

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    private LoaderManager.LoaderCallbacks<UserMoviesHistoryLoader.Result> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<UserMoviesHistoryLoader.Result>() {
                @Override
                public Loader<UserMoviesHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new UserMoviesHistoryLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<UserMoviesHistoryLoader.Result> loader,
                        UserMoviesHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    mAdapter.setData(data.results);
                    setEmptyMessage(data.emptyTextResId);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<UserMoviesHistoryLoader.Result> loader) {
                    // keep current data
                }
            };

    private static class UserMoviesHistoryLoader
            extends GenericSimpleLoader<UserMoviesHistoryLoader.Result> {

        public static class Result {
            public List<HistoryEntry> results;
            public int emptyTextResId;

            public Result(List<HistoryEntry> results, int emptyTextResId) {
                this.results = results;
                this.emptyTextResId = emptyTextResId;
            }
        }

        public UserMoviesHistoryLoader(Context context) {
            super(context);
        }

        @Override
        public Result loadInBackground() {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt == null) {
                return buildResultFailure(R.string.trakt_error_credentials);
            }

            List<HistoryEntry> history;
            try {
                history = trakt.users().historyMovies("me", 1, 25, Extended.IMAGES);
            } catch (RetrofitError e) {
                Timber.e(e, "Loading user movie history failed");
                return buildResultFailure(AndroidUtils.isNetworkConnected(getContext())
                        ? R.string.trakt_error_general : R.string.offline);
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
                return buildResultFailure(R.string.trakt_error_credentials);
            }

            if (history == null) {
                Timber.e("Loading user movie history failed, was null");
                return buildResultFailure(R.string.trakt_error_general);
            } else {
                return new Result(history, R.string.user_movie_stream_empty);
            }
        }

        private static Result buildResultFailure(int emptyTextResId) {
            return new Result(null, emptyTextResId);
        }
    }
}
