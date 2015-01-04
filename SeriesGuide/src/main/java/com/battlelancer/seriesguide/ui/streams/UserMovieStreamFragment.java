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
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsFragment;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
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
    protected int getEmptyMessageResId() {
        return R.string.user_movie_stream_empty;
    }

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new MovieHistoryAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(MoviesActivity.USER_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(MoviesActivity.USER_LOADER_ID, null,
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

    private LoaderManager.LoaderCallbacks<List<HistoryEntry>> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<HistoryEntry>>() {
                @Override
                public Loader<List<HistoryEntry>> onCreateLoader(int id, Bundle args) {
                    return new UserMoviesActivityLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<List<HistoryEntry>> loader,
                        List<HistoryEntry> data) {
                    mAdapter.setData(data);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<List<HistoryEntry>> loader) {
                    // do nothing
                }
            };

    private static class UserMoviesActivityLoader
            extends GenericSimpleLoader<List<HistoryEntry>> {

        public UserMoviesActivityLoader(Context context) {
            super(context);
        }

        @Override
        public List<HistoryEntry> loadInBackground() {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt == null) {
                return null;
            }

            try {
                List<HistoryEntry> history = trakt.users()
                        .historyMovies("me", 1, 25, Extended.IMAGES);

                if (history == null) {
                    Timber.e("Loading user movie history failed, was null");
                    return null;
                }

                return history;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading user movie history failed");
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
            }

            return null;
        }
    }
}
