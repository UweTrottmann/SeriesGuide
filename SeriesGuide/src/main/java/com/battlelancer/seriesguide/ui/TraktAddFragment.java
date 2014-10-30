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
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.AddActivity.AddPagerAdapter;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.Extended;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktAddFragment extends AddFragment {

    public static TraktAddFragment newInstance(int position) {
        TraktAddFragment f = new TraktAddFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("traktlisttype", position);
        f.setArguments(args);

        return f;
    }

    public static class TraktAddResultsEvent {
        public int type;
        public List<SearchResult> results;

        public TraktAddResultsEvent(int type, List<SearchResult> results) {
            this.type = type;
            this.results = results;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_trakt, container, false);
        ButterKnife.inject(this, v);

        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int type = getListType();

        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            // show offline message, abort
            setEmptyMessage(R.string.offline);
            setProgressVisible(false, false);
            return;
        }

        // set empty message
        setEmptyMessage(R.string.add_empty);

        // only create and fill a new adapter if there is no previous one
        // (e.g. after config/page changed)
        if (adapter == null) {
            adapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                    new ArrayList<SearchResult>(), mDetailsButtonListener);

            setProgressVisible(true, false);
            AndroidUtils.executeOnPool(new GetTraktShowsTask(getActivity()), type);
        } else {
            setProgressVisible(false, false);
        }

        if (type == AddPagerAdapter.LIBRARY_TAB_POSITION
                || type == AddPagerAdapter.WATCHLIST_TAB_POSITION) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        String tag = null;
        switch (getListType()) {
            case AddPagerAdapter.TRENDING_TAB_POSITION:
                tag = "Trending";
                break;
            case AddPagerAdapter.LIBRARY_TAB_POSITION:
                tag = "Library";
                break;
            case AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                tag = "Recommended";
                break;
            case AddPagerAdapter.WATCHLIST_TAB_POSITION:
                tag = "Watchlist";
                break;
        }
        if (tag != null) {
            Utils.trackView(getActivity(), tag);
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trakt_library_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_all) {
            if (searchResults != null) {
                TaskManager.getInstance(getActivity()).performAddTask(searchResults, false, false);
            }
            // disable the item so the user has to come back
            item.setEnabled(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getListType() {
        return getArguments().getInt("traktlisttype");
    }

    public void onEventMainThread(TraktAddResultsEvent event) {
        if (event.type == getListType()) {
            setSearchResults(event.results);
            setProgressVisible(false, true);
        }
    }

    public static class GetTraktShowsTask extends AsyncTask<Integer, Void, List<SearchResult>> {

        private Context context;
        private int type;

        public GetTraktShowsTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<SearchResult> doInBackground(Integer... params) {
            Timber.d("Getting shows...");
            type = params[0];
            List<SearchResult> showList = new ArrayList<>();

            List<TvShow> shows = new ArrayList<>();

            if (type == AddPagerAdapter.TRENDING_TAB_POSITION) {
                try {
                    shows = ServiceUtils.getTrakt(context).showService().trending();
                } catch (RetrofitError e) {
                    Timber.e(e, "Loading trending shows failed");
                    // ignored, just display empty list
                }
            } else {
                try {
                    Trakt manager = ServiceUtils.getTraktWithAuth(context);
                    if (manager != null) {
                        switch (type) {
                            case AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                                shows = manager.recommendationsService().shows();
                                break;
                            case AddPagerAdapter.LIBRARY_TAB_POSITION:
                                shows = manager.userService().libraryShowsAll(
                                        TraktCredentials.get(context).getUsername(),
                                        Extended.EXTENDED);
                                break;
                            case AddPagerAdapter.WATCHLIST_TAB_POSITION:
                                shows = manager.userService().watchlistShows(
                                        TraktCredentials.get(context).getUsername());
                                break;
                        }
                    }
                } catch (RetrofitError e) {
                    Timber.e(e, "Loading shows failed");
                    // ignored, just display empty list
                }
            }

            // return empty list right away if there are no results
            if (shows == null || shows.size() == 0) {
                return showList;
            }

            // get a list of existing shows to filter against
            final Cursor existingShows = context.getContentResolver().query(Shows.CONTENT_URI,
                    new String[] {
                            Shows._ID
                    }, null, null, null);
            final HashSet<Integer> existingShowTvdbIds = new HashSet<>();
            if (existingShows != null) {
                while (existingShows.moveToNext()) {
                    existingShowTvdbIds.add(existingShows.getInt(0));
                }
                existingShows.close();
            }

            parseTvShowsToSearchResults(shows, showList, existingShowTvdbIds, context);

            return showList;
        }

        @Override
        protected void onPostExecute(List<SearchResult> results) {
            EventBus.getDefault().post(new TraktAddResultsEvent(type, results));
        }
    }

    /**
     * Parse a list of {@link TvShow} objects to a list of {@link SearchResult} objects.
     */
    private static void parseTvShowsToSearchResults(List<TvShow> inputList,
            List<SearchResult> outputList, HashSet<Integer> existingShowTvdbIds, Context context) {
        // large screens show larger poster, so use a higher resolution variant
        String posterSizeSpec = DisplaySettings.isVeryLargeScreen(context)
                ? TraktSettings.POSTER_SIZE_SPEC_300 : TraktSettings.POSTER_SIZE_SPEC_138;
        // build list
        for (TvShow tvShow : inputList) {
            // only list shows not in the database already
            if (!existingShowTvdbIds.contains(tvShow.tvdb_id)) {
                SearchResult show = new SearchResult();
                show.tvdbid = tvShow.tvdb_id;
                show.title = tvShow.title;
                show.overview = tvShow.overview;
                if (tvShow.images.poster != null) {
                    show.poster = tvShow.images.poster.replace(
                            TraktSettings.POSTER_SIZE_SPEC_DEFAULT, posterSizeSpec);
                }
                outputList.add(show);
            }
        }
    }
}
