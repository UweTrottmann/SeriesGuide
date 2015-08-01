/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.AddActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads either the connected trakt user's recommendations, library or watchlist.
 */
public class TraktAddLoader extends GenericSimpleLoader<TraktAddLoader.Result> {

    public static class Result {
        public List<SearchResult> results;
        public int emptyTextResId;

        public Result(List<SearchResult> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    private final int type;

    public TraktAddLoader(Context context, int type) {
        super(context);
        this.type = type;
    }

    @Override
    public Result loadInBackground() {
        List<Show> shows = new LinkedList<>();
        try {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
            if (trakt != null) {
                switch (type) {
                    case AddActivity.AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                        shows = trakt.recommendations().shows(Extended.IMAGES);
                        break;
                    case AddActivity.AddPagerAdapter.LIBRARY_TAB_POSITION:
                        List<BaseShow> watchedShows = trakt.sync().watchedShows(
                                Extended.IMAGES);
                        extractShows(watchedShows, shows);
                        break;
                    case AddActivity.AddPagerAdapter.WATCHLIST_TAB_POSITION:
                        List<BaseShow> watchlistedShows = trakt.sync()
                                .watchlistShows(Extended.IMAGES);
                        extractShows(watchlistedShows, shows);
                        break;
                    default:
                        // cause NPE if used incorrectly
                        return null;
                }
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading shows failed");
            // only check for network here to allow hitting the response cache
            return buildResultFailure(AndroidUtils.isNetworkConnected(getContext())
                    ? R.string.trakt_error_general : R.string.offline);
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(getContext()).setCredentialsInvalid();
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        // return empty list right away if there are no results
        if (shows == null || shows.size() == 0) {
            return buildResultSuccess(new LinkedList<SearchResult>());
        }

        return buildResultSuccess(parseTraktShowsToSearchResults(getContext(), shows));
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

    private static Result buildResultSuccess(List<SearchResult> results) {
        return new Result(results, R.string.add_empty);
    }

    private static Result buildResultFailure(int errorResId) {
        return new Result(new LinkedList<SearchResult>(), errorResId);
    }

    /**
     * Transforms a list of trakt shows to a list of {@link SearchResult}, filters out shows already
     * in the local database.
     */
    public static List<SearchResult> parseTraktShowsToSearchResults(Context context,
            @NonNull List<Show> traktShows) {
        List<SearchResult> results = new ArrayList<>();

        // build list
        HashSet<Integer> existingShows = ShowTools.getShowTvdbIdsAsSet(context);
        for (Show show : traktShows) {
            if (show.ids == null || show.ids.tvdb == null) {
                // has no TheTVDB id
                continue;
            }
            SearchResult result = new SearchResult();
            result.tvdbid = show.ids.tvdb;
            result.title = show.title;
            result.overview = show.year == null ? "" : String.valueOf(show.year);
            if (show.images != null && show.images.poster != null) {
                result.poster = show.images.poster.thumb;
            }
            if (existingShows != null && existingShows.contains(show.ids.tvdb)) {
                // is already in local database
                result.isAdded = true;
            }
            results.add(result);
        }

        return results;
    }
}
