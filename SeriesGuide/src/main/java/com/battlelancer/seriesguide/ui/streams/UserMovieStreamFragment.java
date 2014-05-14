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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MovieStreamAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsFragment;
import com.battlelancer.seriesguide.ui.MoviesActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.jakewharton.trakt.services.ActivityService;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Displays a stream of movies the user has recently watched on trakt.
 */
public class UserMovieStreamFragment extends StreamFragment {

    private MovieStreamAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Movies You");
    }

    @Override
    protected int getEmptyMessageResId() {
        return R.string.user_movie_stream_empty;
    }

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new MovieStreamAdapter(getActivity());
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

        ActivityItem activity = mAdapter.getItem(position);
        if (activity == null) {
            return;
        }

        // display movie details
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, activity.movie.tmdbId);
        startActivity(i);
    }

    private LoaderManager.LoaderCallbacks<List<ActivityItem>> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<ActivityItem>>() {
                @Override
                public Loader<List<ActivityItem>> onCreateLoader(int id, Bundle args) {
                    return new UserMoviesActivityLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<List<ActivityItem>> loader,
                        List<ActivityItem> data) {
                    mAdapter.setData(data);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<List<ActivityItem>> loader) {
                    // do nothing
                }
            };

    private static class UserMoviesActivityLoader
            extends GenericSimpleLoader<List<ActivityItem>> {

        public UserMoviesActivityLoader(Context context) {
            super(context);
        }

        @Override
        public List<ActivityItem> loadInBackground() {
            Trakt manager = ServiceUtils.getTraktWithAuth(getContext());
            if (manager == null) {
                return null;
            }

            try {
                final ActivityService activityService = manager.activityService();
                // get movies from the last 2 months
                Activity activity = activityService.user(
                        TraktCredentials.get(getContext()).getUsername(),
                        ActivityType.Movie.toString(),
                        ActivityAction.Watching + ","
                                + ActivityAction.Checkin + ","
                                + ActivityAction.Scrobble,
                        (System.currentTimeMillis() - 8 * DateUtils.WEEK_IN_MILLIS) / 1000,
                        null,
                        null
                );

                if (activity == null || activity.activity == null) {
                    Timber.e("Loading user movie activity failed, was null");
                    return null;
                }

                return activity.activity;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading user movie activity failed");
            }

            return null;
        }
    }
}
