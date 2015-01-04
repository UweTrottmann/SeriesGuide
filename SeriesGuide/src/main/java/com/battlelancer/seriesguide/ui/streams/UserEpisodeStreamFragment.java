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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.EpisodeHistoryAdapter;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
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
 * Displays the latest trakt activity of the user.
 */
public class UserEpisodeStreamFragment extends StreamFragment {

    private EpisodeHistoryAdapter mAdapter;

    @Override
    protected int getEmptyMessageResId() {
        return R.string.user_stream_empty;
    }

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new EpisodeHistoryAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(ShowsActivity.USER_LOADER_ID, null, mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(ShowsActivity.USER_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not respond if we get a header position (e.g. shortly after data was refreshed)
        if (position < 0) {
            return;
        }

        HistoryEntry item = mAdapter.getItem(position);
        if (item == null) {
            return;
        }

        if (item.episode == null || item.episode.season == null || item.episode.number == null
                || item.show == null || item.show.ids == null || item.show.ids.tvdb == null) {
            // no episode or show? give up
            return;
        }

        Cursor episodeQuery = getActivity().getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(item.show.ids.tvdb),
                new String[] {
                        SeriesGuideContract.Episodes._ID
                }, SeriesGuideContract.Episodes.NUMBER + "=" + item.episode.number + " AND "
                        + SeriesGuideContract.Episodes.SEASON + "=" + item.episode.season, null,
                null
        );
        if (episodeQuery == null) {
            return;
        }

        if (episodeQuery.getCount() != 0) {
            // display the episode details if we have a match
            episodeQuery.moveToFirst();
            showDetails(view, episodeQuery.getInt(0));
        } else {
            // offer to add the show if it's not in the show database yet
            SearchResult showToAdd = new SearchResult();
            showToAdd.tvdbid = item.show.ids.tvdb;
            showToAdd.title = item.show.title;
            showToAdd.overview = item.show.overview;
            AddShowDialogFragment.showAddDialog(showToAdd, getFragmentManager());
        }

        episodeQuery.close();
    }

    private LoaderManager.LoaderCallbacks<List<HistoryEntry>> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<List<HistoryEntry>>() {
                @Override
                public Loader<List<HistoryEntry>> onCreateLoader(int id, Bundle args) {
                    return new UserEpisodeActivityLoader(getActivity());
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

    private static class UserEpisodeActivityLoader extends GenericSimpleLoader<List<HistoryEntry>> {

        public UserEpisodeActivityLoader(Context context) {
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
                        .historyEpisodes("me", 1, 25, Extended.IMAGES);

                if (history == null) {
                    Timber.e("Loading user episode history failed, was null");
                    return null;
                }

                return history;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading user episode history failed");
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
            }

            return null;
        }
    }
}
