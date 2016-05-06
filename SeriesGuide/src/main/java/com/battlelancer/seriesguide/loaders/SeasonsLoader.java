/*
 * Copyright 2016 Uwe Trottmann
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
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads show and season info based on a given episode.
 */
public class SeasonsLoader extends GenericSimpleLoader<SeasonsLoader.Result> {

    private final int episodeTvdbId;

    public static class Result {
        public int showTvdbId;
        public String showTitle;
        public String showPoster;
        public int seasonIndexOfEpisode;
        @NonNull
        public List<Season> seasonsOfShow;

        public Result(int showTvdbId, String showTitle, String showPoster,
                int seasonIndexOfEpisode, @NonNull List<Season> seasonsOfShow) {
            this.showTvdbId = showTvdbId;
            this.showTitle = showTitle;
            this.showPoster = showPoster;
            this.seasonIndexOfEpisode = seasonIndexOfEpisode;
            this.seasonsOfShow = seasonsOfShow;
        }
    }

    public SeasonsLoader(Context context, int episodeTvdbId) {
        super(context);
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    public Result loadInBackground() {
        // query for show and season info of the given episode
        Cursor episodeQuery = getContext().getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeWithShowUri(String.valueOf(episodeTvdbId)),
                EpisodeQuery.PROJECTION, null, null, null);
        if (episodeQuery == null) {
            return null;
        }
        if (!episodeQuery.moveToFirst()) {
            episodeQuery.close();
            return null;
        }

        int showTvdbId = episodeQuery.getInt(EpisodeQuery.SHOW_TVDB_ID);
        String showTitle = episodeQuery.getString(EpisodeQuery.SHOW_TITLE);
        String showPoster = episodeQuery.getString(EpisodeQuery.SHOW_POSTER);
        int seasonTvdbId = episodeQuery.getInt(EpisodeQuery.SEASON_TVDB_ID);

        episodeQuery.close();

        // query for seasons of the show
        List<Season> seasons = new ArrayList<>();
        Cursor seasonsQuery = getContext().getContentResolver()
                .query(SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId),
                        SeasonsQuery.PROJECTION, null, null,
                        DisplaySettings.getSeasonSortOrder(getContext()).query());
        int seasonIndex = 0;
        int seasonIndexOfEpisode = 0;
        if (seasonsQuery != null) {
            while (seasonsQuery.moveToNext()) {
                Season season = new Season();
                season.tvdbId = seasonsQuery.getInt(SeasonsQuery.ID);
                season.season = seasonsQuery.getInt(SeasonsQuery.NUMBER);
                seasons.add(season);

                if (season.tvdbId == seasonTvdbId) {
                    seasonIndexOfEpisode = seasonIndex;
                }
                seasonIndex++;
            }
            seasonsQuery.close();
        }

        return new Result(showTvdbId, showTitle, showPoster, seasonIndexOfEpisode,
                seasons);
    }

    interface EpisodeQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Shows.REF_SHOW_ID,
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.POSTER,
                SeriesGuideContract.Seasons.REF_SEASON_ID,
        };
        int SHOW_TVDB_ID = 0;
        int SHOW_TITLE = 1;
        int SHOW_POSTER = 2;
        int SEASON_TVDB_ID = 3;
    }

    interface SeasonsQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Seasons._ID,
                SeriesGuideContract.Seasons.COMBINED
        };
        int ID = 0;
        int NUMBER = 1;
    }
}
