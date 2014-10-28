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
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.AddActivity.AddPagerAdapter;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.TrendingShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
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
                    new ArrayList<SearchResult>());

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

            List<Show> shows = new LinkedList<>();

            try {
                if (type == AddPagerAdapter.TRENDING_TAB_POSITION) {
                    List<TrendingShow> trendingShows = ServiceUtils.getTraktV2(context)
                            .shows()
                            .trending(null, null, Extended.IMAGES);
                    for (TrendingShow show : trendingShows) {
                        if (show.show == null || show.show.ids == null
                                || show.show.ids.tvdb == null) {
                            // skip if required values are missing
                            continue;
                        }
                        shows.add(show.show);
                    }
                } else {
                    TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
                    if (trakt != null) {
                        switch (type) {
                            case AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                                shows = trakt.recommendations().shows(Extended.IMAGES);
                                break;
                            case AddPagerAdapter.LIBRARY_TAB_POSITION:
                                List<BaseShow> watchedShows = trakt.sync().watchedShows(
                                        Extended.IMAGES);
                                extractShows(watchedShows, shows);
                                break;
                            case AddPagerAdapter.WATCHLIST_TAB_POSITION:
                                List<BaseShow> watchlistedShows = trakt.sync()
                                        .watchlistShows(Extended.IMAGES);
                                extractShows(watchlistedShows, shows);
                                break;
                        }
                    }
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Loading shows failed: " + e.getUrl());
                // ignored, just display empty list
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
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

            parseTvShowsToSearchResults(context, shows, showList, existingShowTvdbIds);

            return showList;
        }

        private void extractShows(List<BaseShow> watchedShows, List<Show> shows) {
            for (BaseShow show : watchedShows) {
                if (show.show == null || show.show.ids == null
                        || show.show.ids.tvdb == null) {
                    continue; // skip if required values are missing
                }
                shows.add(show.show);
            }
        }

        @Override
        protected void onPostExecute(List<SearchResult> results) {
            EventBus.getDefault().post(new TraktAddResultsEvent(type, results));
        }
    }

    /**
     * Transform a list of trakt shows to a list of {@link SearchResult}.
     */
    private static void parseTvShowsToSearchResults(Context context, List<Show> inputList,
            List<SearchResult> outputList, HashSet<Integer> existingShowTvdbIds) {
        // build list
        for (Show show : inputList) {
            // only list shows not in the database already
            if (!existingShowTvdbIds.contains(show.ids.tvdb)) {
                SearchResult result = new SearchResult();
                result.tvdbid = show.ids.tvdb;
                result.title = show.title;
                result.overview = String.valueOf(show.year);
                if (show.images != null && show.images.poster != null) {
                    result.poster = show.images.poster.thumb;
                }
                outputList.add(result);
            }
        }
    }
}
