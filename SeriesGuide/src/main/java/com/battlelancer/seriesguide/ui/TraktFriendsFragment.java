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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.jakewharton.trakt.services.ActivityService;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktFriendsFragment extends SherlockFragment implements
        AdapterView.OnItemClickListener {

    private TraktFriendsAdapter mAdapter;

    private View mContentContainer;

    private GridView mGridView;

    private View mProgressBar;

    private TextView mEmptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friends, container, false);

        mContentContainer = v.findViewById(R.id.contentContainer);
        mProgressBar = v.findViewById(R.id.progressIndicator);

        mEmptyView = (TextView) v.findViewById(R.id.emptyViewFriends);
        mGridView = (GridView) v.findViewById(android.R.id.list);
        mGridView.setOnItemClickListener(this);
        mGridView.setEmptyView(mEmptyView);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // abort if offline
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            mEmptyView.setText(R.string.offline);
            setProgressLock(false);
            return;
        }

        setProgressLock(true);
        mEmptyView.setText(R.string.friends_empty);

        if (mAdapter == null) {
            mAdapter = new TraktFriendsAdapter(getActivity());
        }
        mGridView.setAdapter(mAdapter);

        getLoaderManager().initLoader(ShowsActivity.FRIENDS_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Friends");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ActivityItem activity = (ActivityItem) mGridView.getItemAtPosition(position);
        if (activity == null) {
            return;
        }

        Cursor episodeQuery = getActivity().getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(activity.show.tvdb_id), new String[] {
                        Episodes._ID
                }, Episodes.NUMBER + "=" + activity.episode.number + " AND "
                        + Episodes.SEASON + "=" + activity.episode.season, null, null
        );
        if (episodeQuery == null) {
            return;
        }

        if (episodeQuery.getCount() != 0) {
            // display the episode details if we have a match
            episodeQuery.moveToFirst();
            showDetails(episodeQuery.getInt(0));
        } else {
            // offer to add the show if it's not in the show database yet
            SearchResult showToAdd = new SearchResult();
            showToAdd.tvdbid = activity.show.tvdb_id;
            showToAdd.title = activity.show.title;
            showToAdd.overview = activity.show.overview;
            AddDialogFragment.showAddDialog(showToAdd, getFragmentManager());
        }

        episodeQuery.close();
    }

    private void showDetails(int episodeId) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
    }

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
                    return new ArrayList<>();
                }

                return activity.activity;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading friends activity failed");
            }

            return new ArrayList<>();
        }
    }

    private static class TraktFriendsAdapter extends ArrayAdapter<ActivityItem> {

        private final LayoutInflater mInflater;

        public TraktFriendsAdapter(Context context) {
            super(context, R.layout.friend);
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
            // A ViewHolder keeps references to children views to avoid
            // unnecessary calls to findViewById() on each row.
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.friend, parent, false);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.show = (TextView) convertView.findViewById(R.id.show);
                holder.episode = (TextView) convertView.findViewById(R.id.episode);
                holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);
                holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder.
            ActivityItem activity = getItem(position);

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

            ImageView avatar;
        }
    }

    public void setProgressLock(boolean isLocked) {
        mContentContainer.setVisibility(isLocked ? View.GONE : View.VISIBLE);
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
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
                    setProgressLock(false);
                }

                @Override
                public void onLoaderReset(Loader<List<ActivityItem>> loader) {
                    // do nothing
                }
            };
}
