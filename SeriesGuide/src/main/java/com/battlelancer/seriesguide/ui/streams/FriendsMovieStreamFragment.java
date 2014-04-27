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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktSettings;
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
 * Displays a stream of movies the user's trakt friends have recently watched.
 */
public class FriendsMovieStreamFragment extends StreamFragment {

    private FriendsMovieActivityAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Movies Friends");
    }

    @Override
    protected int getEmptyMessageResId() {
        return R.string.friends_empty;
    }

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new FriendsMovieActivityAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(MoviesActivity.FRIENDS_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(MoviesActivity.FRIENDS_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ActivityItem activity = (ActivityItem) mGridView.getItemAtPosition(position);
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
                    return new FriendsMoviesActivityLoader(getActivity());
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

    private static class FriendsMoviesActivityLoader
            extends GenericSimpleLoader<List<ActivityItem>> {

        public FriendsMoviesActivityLoader(Context context) {
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
                Activity activity = activityService.friends(ActivityType.Movie.toString(),
                        ActivityAction.Watching + ","
                                + ActivityAction.Checkin + ","
                                + ActivityAction.Scrobble,
                        (System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) / 1000, null, null
                );

                if (activity == null || activity.activity == null) {
                    Timber.e("Loading friends movie activity failed, was null");
                    return null;
                }

                return activity.activity;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading friends movie activity failed");
            }

            return null;
        }
    }

    private static class FriendsMovieActivityAdapter extends ArrayAdapter<ActivityItem> {

        private final LayoutInflater mInflater;

        public FriendsMovieActivityAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<ActivityItem> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to child views to avoid
            // unnecessary calls to findViewById() on each row.
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.friend, parent, false);

                holder = new ViewHolder();
                holder.timestamp = (TextView) convertView.findViewById(
                        R.id.textViewFriendTimestamp);
                holder.movie = (TextView) convertView.findViewById(R.id.textViewFriendShow);
                holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
                holder.username = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
                holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);

                // no need for secondary text
                convertView.findViewById(R.id.textViewFriendEpisode).setVisibility(View.GONE);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            ActivityItem activity = getItem(position);

            // movie poster
            if (activity.movie.images != null && !TextUtils.isEmpty(activity.movie.images.poster)) {
                String posterPath = activity.movie.images.poster.replace(
                        TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
                ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
            }

            holder.username.setText(activity.user.username);
            ServiceUtils.getPicasso(getContext()).load(activity.user.avatar).into(holder.avatar);

            holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Small_Dim);

            CharSequence timestamp;
            // friend is watching something right now?
            if (activity.action == ActivityAction.Watching) {
                timestamp = getContext().getString(R.string.now);
                holder.timestamp.setTextAppearance(getContext(),
                        R.style.TextAppearance_Small_Highlight_Red);
            } else {
                timestamp = DateUtils.getRelativeTimeSpanString(
                        activity.timestamp.getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            }

            holder.movie.setText(activity.movie.title);
            holder.timestamp.setText(timestamp);

            return convertView;
        }

        static class ViewHolder {

            TextView timestamp;

            TextView movie;

            ImageView poster;

            TextView username;

            ImageView avatar;
        }
    }
}
