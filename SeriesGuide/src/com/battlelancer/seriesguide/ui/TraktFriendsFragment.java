
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.MediaEntity;
import com.jakewharton.trakt.entities.UserProfile;
import com.jakewharton.trakt.entities.WatchedMediaEntity;
import com.jakewharton.trakt.enumerations.MediaType;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class TraktFriendsFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<UserProfile>> {

    private TraktFriendsAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // TODO put into res
        setEmptyText("Try again later or you might just have no friends on trakt, yet.");

        mAdapter = new TraktFriendsAdapter(getActivity());
        setListAdapter(mAdapter);

        setListShown(false);

        getLoaderManager().initLoader(0, null, this);

    }

    private static class TraktFriendsLoader extends AsyncTaskLoader<List<UserProfile>> {

        private List<UserProfile> mFriends;

        public TraktFriendsLoader(Context context) {
            super(context);
        }

        @Override
        public List<UserProfile> loadInBackground() {
            if (ShareUtils.isTraktCredentialsValid(getContext())) {
                ServiceManager manager = null;
                try {
                    manager = Utils.setupServiceManager(getContext());
                } catch (Exception e) {
                    // TODO
                    return null;
                }

                try {
                    List<UserProfile> friends = manager.userService()
                            .friends(Utils.getTraktUsername(getContext())).fire();
                    return friends;
                } catch (TraktException te) {
                    // TODO
                    return null;
                } catch (ApiException ae) {
                    // TODO
                    return null;
                }
            } else {
                // TODO
                return null;
            }
        }

        /**
         * Called when there is new data to deliver to the client. The super
         * class will take care of delivering it; the implementation here just
         * adds a little more logic.
         */
        @Override
        public void deliverResult(List<UserProfile> friends) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (friends != null) {
                    onReleaseResources(friends);
                }
            }
            List<UserProfile> oldFriends = friends;
            mFriends = friends;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(friends);
            }

            if (oldFriends != null) {
                onReleaseResources(oldFriends);
            }
        }

        @Override
        protected void onStartLoading() {
            if (mFriends != null) {
                deliverResult(mFriends);
            } else {
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(List<UserProfile> friends) {
            super.onCanceled(friends);

            onReleaseResources(friends);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release resources
            if (mFriends != null) {
                onReleaseResources(mFriends);
                mFriends = null;
            }
        }

        /**
         * Helper function to take care of releasing resources associated with
         * an actively loaded data set.
         */
        protected void onReleaseResources(List<UserProfile> apps) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }
    }

    private static class TraktFriendsAdapter extends ArrayAdapter<UserProfile> {
        private final LayoutInflater mInflater;

        private final SharedPreferences mPrefs;

        public TraktFriendsAdapter(Context context) {
            super(context, R.layout.friend);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
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
            // unneccessary calls to findViewById() on each row.
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.friend, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.show = (TextView) convertView.findViewById(R.id.show);
                holder.episode = (TextView) convertView.findViewById(R.id.episode);
                holder.timestamp = (TextView) convertView.findViewById(R.id.timestamp);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // TODO refactor!
            // Bind the data efficiently with the holder.
            UserProfile friend = getItem(position);
            holder.name.setText(friend.username);
            // TODO avatar

            String show = "";
            String episode = "";
            String timestamp = "";
            if (friend.watching != null) {
                // look if this friend is watching something right now
                WatchedMediaEntity watching = friend.watching;
                switch (watching.type) {
                    case TvShow:
                        show = watching.show.title;
                        String episodenumber = Utils.getEpisodeNumber(mPrefs,
                                String.valueOf(watching.episode.season),
                                String.valueOf(watching.episode.number));
                        episode = episodenumber + " " + watching.episode.title;
                        timestamp = getContext().getString(R.string.now);
                        break;
                }
            } else if (friend.watched != null) {
                // if not display the latest episode he watched
                List<MediaEntity> watched = friend.watched;
                MediaEntity latestShow = null;
                for (MediaEntity mediaEntity : watched) {
                    if (mediaEntity.type == MediaType.TvShow) {
                        latestShow = mediaEntity;
                        break;
                    }
                }

                if (latestShow != null) {
                    show = latestShow.show.title;
                    String episodenumber = Utils.getEpisodeNumber(mPrefs,
                            String.valueOf(latestShow.episode.season),
                            String.valueOf(latestShow.episode.number));
                    episode = episodenumber + " " + latestShow.episode.title;
                    timestamp = "TODO";
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
