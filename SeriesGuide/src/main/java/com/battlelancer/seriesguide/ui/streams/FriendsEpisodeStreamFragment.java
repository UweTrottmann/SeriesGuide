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
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.EpisodeStreamAdapter;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.ActivityItem;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.List;

public class FriendsEpisodeStreamFragment extends StreamFragment {

    private EpisodeStreamAdapter mAdapter;

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
            mAdapter = new EpisodeStreamAdapter(getActivity());
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
                    return new FriendsEpisodeActivityLoader(getActivity());
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

    private static class FriendsEpisodeActivityLoader
            extends GenericSimpleLoader<List<ActivityItem>> {

        public FriendsEpisodeActivityLoader(Context context) {
            super(context);
        }

        @Override
        public List<ActivityItem> loadInBackground() {
            //Trakt manager = ServiceUtils.getTraktWithAuth(getContext());
            //if (manager == null) {
            //    return null;
            //}
            //
            //try {
            //    final ActivityService activityService = manager.activityService();
            //    Activity activity = activityService.friends(ActivityType.Episode.toString(),
            //            ActivityAction.Watching + ","
            //                    + ActivityAction.Checkin + ","
            //                    + ActivityAction.Scrobble,
            //            (System.currentTimeMillis() - 7 * DateUtils.DAY_IN_MILLIS) / 1000, null, null
            //    );
            //
            //    if (activity == null || activity.activity == null) {
            //        Timber.e("Loading friends episode activity failed, was null");
            //        return null;
            //    }
            //
            //    return activity.activity;
            //} catch (RetrofitError e) {
            //    Timber.e(e, "Loading friends episode activity failed");
            //}

            return null;
        }
    }
}
