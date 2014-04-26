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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktSettings;
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

public class TraktFriendsFragment extends StreamFragment {

    private TraktFriendsAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Friends");
    }

    @Override
    protected int getEmptyMessageResId() {
        return R.string.friends_empty;
    }

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new TraktFriendsAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(ShowsActivity.FRIENDS_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(ShowsActivity.FRIENDS_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<List<ActivityItem>> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<ActivityItem>>() {
                @Override
                public Loader<List<ActivityItem>> onCreateLoader(int id, Bundle args) {
                    return new TraktFriendsLoader(getActivity());
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

    private static class TraktFriendsLoader extends GenericSimpleLoader<List<ActivityItem>> {

        public TraktFriendsLoader(Context context) {
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
                Activity activity = activityService.friends(ActivityType.Episode.toString(),
                        ActivityAction.Watching + ","
                                + ActivityAction.Checkin + ","
                                + ActivityAction.Scrobble,
                        (System.currentTimeMillis() - DateUtils.WEEK_IN_MILLIS) / 1000, null, null
                );

                if (activity == null || activity.activity == null) {
                    Timber.e("Loading friends activity failed, was null");
                    return null;
                }

                return activity.activity;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading friends activity failed");
            }

            return null;
        }
    }

    private static class TraktFriendsAdapter extends ArrayAdapter<ActivityItem> {

        private final LayoutInflater mInflater;

        public TraktFriendsAdapter(Context context) {
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
                holder.name = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
                holder.show = (TextView) convertView.findViewById(R.id.textViewFriendShow);
                holder.episode = (TextView) convertView.findViewById(R.id.textViewFriendEpisode);
                holder.timestamp = (TextView) convertView.findViewById(
                        R.id.textViewFriendTimestamp);
                holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
                holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            ActivityItem activity = getItem(position);

            // show poster
            if (activity.show.images != null && !TextUtils.isEmpty(activity.show.images.poster)) {
                String posterPath = activity.show.images.poster.replace(
                        TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
                ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
            }

            holder.name.setText(activity.user.username);
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

            holder.show.setText(activity.show.title);
            holder.episode.setText(Utils.getNextEpisodeString(getContext(), activity.episode.season,
                    activity.episode.number, activity.episode.title));
            holder.timestamp.setText(timestamp);

            return convertView;
        }

        static class ViewHolder {

            TextView name;

            TextView show;

            TextView episode;

            TextView timestamp;

            ImageView poster;

            ImageView avatar;
        }
    }
}
