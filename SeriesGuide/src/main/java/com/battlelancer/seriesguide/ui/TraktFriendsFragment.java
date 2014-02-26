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
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.ActivityItemBase;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.entities.UserProfile;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.jakewharton.trakt.services.UserService;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktFriendsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<List<UserProfile>>, AdapterView.OnItemClickListener {

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

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Friends");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        UserProfile friend = (UserProfile) mGridView.getItemAtPosition(position);

        TvShow show = null;
        TvShowEpisode episode = null;

        if (friend.watching != null) {
            show = friend.watching.show;
            episode = friend.watching.episode;
        } else if (!friend.watched.isEmpty()) {
            ActivityItem activity = friend.watched.get(0);
            show = activity.show;
            episode = activity.episode;
        }

        if (episode != null && show != null) {
            Cursor episodeidquery = getActivity().getContentResolver().query(
                    Episodes.buildEpisodesOfShowUri(show.tvdb_id), new String[]{
                    Episodes._ID
            }, Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[]{
                    String.valueOf(episode.number), String.valueOf(episode.season)
            }, null);

            if (episodeidquery.getCount() != 0) {
                // display the episode details if we have a match
                episodeidquery.moveToFirst();

                int episodeId = episodeidquery.getInt(0);
                showDetails(episodeId);
            } else {
                // offer to add the show if it's not in the show database yet
                SearchResult newshow = new SearchResult();
                newshow.tvdbid = show.tvdb_id;
                newshow.title = show.title;
                newshow.overview = show.overview;
                AddDialogFragment.showAddDialog(newshow, getFragmentManager());
            }

            episodeidquery.close();
        }
    }

    private void showDetails(int episodeId) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
    }

    private static class TraktFriendsLoader extends GenericSimpleLoader<List<UserProfile>> {

        public TraktFriendsLoader(Context context) {
            super(context);
        }

        @Override
        public List<UserProfile> loadInBackground() {
            Trakt manager = ServiceUtils.getTraktWithAuth(getContext());
            if (manager == null) {
                return null;
            }

            List<UserProfile> friendsActivity = new ArrayList<UserProfile>();

            try {
                final UserService userService = manager.userService();
                List<UserProfile> friends = userService
                        .friends(TraktCredentials.get(getContext()).getUsername());

                for (UserProfile friend : friends) {
                    // get the detailed profile
                    UserProfile profile = userService.profile(friend.username);

                    if (profile.watching != null
                            && profile.watching.type == ActivityType.Episode) {
                        // followed is watching something now
                        friendsActivity.add(profile);
                    } else {
                        // look if followed was watching something in the last 4 weeks
                        for (ActivityItem activity : profile.watched) {
                            // only look for episodes
                            if (activity != null && activity.type == ActivityType.Episode) {
                                // is this activity no longer than 4 weeks old
                                // and not in the future?
                                long watchedTime = activity.watched.getTime();
                                if (watchedTime > System.currentTimeMillis()
                                        - DateUtils.WEEK_IN_MILLIS * 4
                                        && watchedTime <= System.currentTimeMillis()) {
                                    UserProfile clonedfriend = new UserProfile();
                                    clonedfriend.username = profile.username;
                                    clonedfriend.avatar = profile.avatar;

                                    List<ActivityItem> watchedclone
                                            = new ArrayList<ActivityItem>();
                                    watchedclone.add(activity);
                                    clonedfriend.watched = watchedclone;

                                    friendsActivity.add(clonedfriend);

                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Loading friends activity failed");
            }

            return friendsActivity;
        }
    }

    private static class TraktFriendsAdapter extends ArrayAdapter<UserProfile> {

        private final LayoutInflater mInflater;

        public TraktFriendsAdapter(Context context) {
            super(context, R.layout.friend);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<UserProfile> data) {
            clear();
            if (data != null) {
                for (UserProfile userProfile : data) {
                    add(userProfile);
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid
            // unnecessary calls to findViewById() on each row.
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.friend, null);

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
            UserProfile friend = getItem(position);

            holder.name.setText(friend.username);
            ServiceUtils.getPicasso(getContext()).load(friend.avatar).into(holder.avatar);

            holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Small_Dim);

            String show = "";
            String episode = "";
            String timestamp = "";
            if (friend.watching != null) {
                // look if this friend is watching something right now
                ActivityItemBase watching = friend.watching;
                switch (watching.type) {
                    case Episode:
                        show = watching.show.title;
                        String episodenumber = Utils.getEpisodeNumber(getContext(),
                                watching.episode.season, watching.episode.number);
                        episode = episodenumber + " " + watching.episode.title;
                        timestamp = getContext().getString(R.string.now);
                        holder.timestamp.setTextAppearance(getContext(),
                                R.style.TextAppearance_Small_Highlight_Red);
                        break;
                    default:
                        break;
                }
            } else if (friend.watched != null) {
                // if not display the latest episode he watched
                List<ActivityItem> watched = friend.watched;
                ActivityItem latestShow = null;
                for (ActivityItem mediaEntity : watched) {
                    if (mediaEntity.type == ActivityType.Episode) {
                        latestShow = mediaEntity;
                        break;
                    }
                }

                if (latestShow != null) {
                    show = latestShow.show.title;
                    String episodenumber = Utils.getEpisodeNumber(getContext(),
                            latestShow.episode.season, latestShow.episode.number);
                    episode = episodenumber + " " + latestShow.episode.title;
                    timestamp = (String) DateUtils.getRelativeTimeSpanString(
                            latestShow.watched.getTime(), System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
                }
            }

            holder.show.setText(show);
            holder.episode.setText(episode);
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

    @Override
    public Loader<List<UserProfile>> onCreateLoader(int id, Bundle args) {
        return new TraktFriendsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<UserProfile>> loader, List<UserProfile> data) {
        mAdapter.setData(data);
        setProgressLock(false);
    }

    @Override
    public void onLoaderReset(Loader<List<UserProfile>> loader) {
        mAdapter.setData(null);
    }

    public void setProgressLock(boolean isLocked) {
        mContentContainer.setVisibility(isLocked ? View.GONE : View.VISIBLE);
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
    }
}
