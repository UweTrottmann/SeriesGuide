/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.util.ImageDownloader;
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
import com.uwetrottmann.seriesguide.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;

public class TraktFriendsFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<UserProfile>> {

    public static final String TAG = "TraktFriendsFragment";

    private TraktFriendsAdapter mAdapter;

    private boolean mDualPane;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFragment = getActivity().findViewById(R.id.fragment_details);
        mDualPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;

        mAdapter = new TraktFriendsAdapter(getActivity());
        setListAdapter(mAdapter);
        final ListView list = getListView();
        list.setDivider(null);
        if (SeriesGuidePreferences.THEME != R.style.AndroidTheme) {
            list.setSelector(R.drawable.list_selector_sg);
        }
        list.setClipToPadding(!AndroidUtils.isHoneycombOrHigher());
        final float scale = getResources().getDisplayMetrics().density;
        int layoutPadding = (int) (10 * scale + 0.5f);
        int defaultPadding = (int) (8 * scale + 0.5f);
        list.setPadding(layoutPadding, layoutPadding, layoutPadding, defaultPadding);

        // nag about no connectivity
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            setEmptyText(getString(R.string.offline));
            setListShown(true);
        } else {
            setEmptyText(getString(R.string.friends_empty));
            setListShown(false);
            getLoaderManager().initLoader(0, null, this);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Friends");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        UserProfile friend = (UserProfile) getListView().getItemAtPosition(position);

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
                showDetails(episodeId, v);
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

    @TargetApi(16)
    private void showDetails(int episodeId, View sourceView) {
        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            EpisodeDetailsFragment detailsFragment = (EpisodeDetailsFragment) getFragmentManager()
                    .findFragmentById(R.id.fragment_details);
            if (detailsFragment == null || detailsFragment.getEpisodeTvdbId() != episodeId) {
                // Make new fragment to show this selection.
                detailsFragment = EpisodeDetailsFragment.newInstance(episodeId, true, true);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.fragment_slide_right_enter,
                        R.anim.fragment_slide_right_exit);
                ft.replace(R.id.fragment_details, detailsFragment, "fragmentDetails").commit();
            }
        } else {
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodesActivity.class);
            intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
        }
    }

    private static class TraktFriendsLoader extends GenericSimpleLoader<List<UserProfile>> {

        public TraktFriendsLoader(Context context) {
            super(context);
        }

        @Override
        public List<UserProfile> loadInBackground() {
            if (TraktSettings.hasTraktCredentials(getContext())) {
                Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(getContext(), false);
                if (manager == null) {
                    return null;
                }

                List<UserProfile> friendsActivity = new ArrayList<UserProfile>();

                try {
                    final UserService userService = manager.userService();
                    List<UserProfile> friends = userService
                            .friends(TraktSettings.getUsername(getContext()));

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
                    Log.w(TAG, e);
                }

                return friendsActivity;
            }

            return null;
        }
    }

    private static class TraktFriendsAdapter extends ArrayAdapter<UserProfile> {

        private final ImageDownloader mImageDownloader;

        private final LayoutInflater mInflater;

        public TraktFriendsAdapter(Context context) {
            super(context, R.layout.friend);
            mImageDownloader = ImageDownloader.getInstance(context);
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
            mImageDownloader.downloadAndStore(friend.avatar, holder.avatar);

            holder.timestamp.setTextColor(Color.GRAY);

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
                        holder.timestamp.setTextColor(Color.RED);
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

        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<UserProfile>> loader) {
        mAdapter.setData(null);
    }

}
