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
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.HistoryActivity;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Displays the latest trakt episode activity of the user.
 */
public class UserEpisodeStreamFragment extends StreamFragment {

    private EpisodeHistoryAdapter mAdapter;

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new EpisodeHistoryAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(HistoryActivity.EPISODES_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(HistoryActivity.EPISODES_LOADER_ID, null,
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
            AddShowDialogFragment.showAddDialog(item.show.ids.tvdb, getFragmentManager());
        }

        episodeQuery.close();
    }

    private LoaderManager.LoaderCallbacks<UserEpisodeHistoryLoader.Result> mActivityLoaderCallbacks
            =
            new LoaderManager.LoaderCallbacks<UserEpisodeHistoryLoader.Result>() {
                @Override
                public Loader<UserEpisodeHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new UserEpisodeHistoryLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<UserEpisodeHistoryLoader.Result> loader,
                        UserEpisodeHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    mAdapter.setData(data.results);
                    setEmptyMessage(data.emptyTextResId);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<UserEpisodeHistoryLoader.Result> loader) {
                    // keep current data
                }
            };

    private static class UserEpisodeHistoryLoader
            extends GenericSimpleLoader<UserEpisodeHistoryLoader.Result> {

        public static class Result {
            public List<HistoryEntry> results;
            public int emptyTextResId;

            public Result(List<HistoryEntry> results, int emptyTextResId) {
                this.results = results;
                this.emptyTextResId = emptyTextResId;
            }
        }

        public UserEpisodeHistoryLoader(Context context) {
            super(context);
        }

        @Override
        public Result loadInBackground() {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt == null) {
                return buildResultFailure(R.string.trakt_error_credentials);
            }

            List<HistoryEntry> history;
            try {
                history = trakt.users().historyEpisodes("me", 1, 25, Extended.IMAGES);
            } catch (RetrofitError e) {
                Timber.e(e, "Loading user episode history failed");
                return buildResultFailure(AndroidUtils.isNetworkConnected(getContext())
                        ? R.string.trakt_error_general : R.string.offline);
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(getContext()).setCredentialsInvalid();
                return buildResultFailure(R.string.trakt_error_credentials);
            }

            if (history == null) {
                Timber.e("Loading user episode history failed, was null");
                return buildResultFailure(R.string.trakt_error_general);
            } else {
                return new Result(history, R.string.user_stream_empty);
            }
        }

        private static Result buildResultFailure(int emptyTextResId) {
            return new Result(null, emptyTextResId);
        }
    }
}
