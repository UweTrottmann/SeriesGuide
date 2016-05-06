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
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.items.Episode;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;

/**
 * Loads episode of a season from the database and returns them as a list of {@link Episode}
 * objects. Also looks for the given episode in the list and returns its index.
 */
public class SeasonEpisodesLoader extends GenericSimpleLoader<SeasonEpisodesLoader.Result> {

    private final int seasonTvdbId;
    private final int seasonNumber;
    private final int episodeTvdbId;

    public static class Result {
        @NonNull
        public ArrayList<Episode> episodes;
        public int requestedEpisodeIndex;

        public Result(@NonNull ArrayList<Episode> episodes, int requestedEpisodeIndex) {
            this.episodes = episodes;
            this.requestedEpisodeIndex = requestedEpisodeIndex;
        }
    }

    public SeasonEpisodesLoader(Context context, int seasonTvdbId, int seasonNumber,
            int episodeTvdbId) {
        super(context);
        this.seasonTvdbId = seasonTvdbId;
        this.seasonNumber = seasonNumber;
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    public Result loadInBackground() {
        // get episodes of season
        Constants.EpisodeSorting sortOrder = DisplaySettings.getEpisodeSortOrder(getContext());
        Cursor episodesOfSeason = getContext().getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(seasonTvdbId)), new String[] {
                        Episodes._ID, Episodes.NUMBER
                }, null, null, sortOrder.query()
        );

        ArrayList<Episode> episodes = new ArrayList<>();
        int requestedEpisodeIndex = 0;
        if (episodesOfSeason != null) {
            int i = 0;
            while (episodesOfSeason.moveToNext()) {
                int curEpisodeId = episodesOfSeason.getInt(0);
                if (curEpisodeId == episodeTvdbId) {
                    requestedEpisodeIndex = i;
                }
                Episode episode = new Episode();
                episode.episodeId = curEpisodeId;
                episode.episodeNumber = episodesOfSeason.getInt(1);
                episode.seasonNumber = seasonNumber;
                episodes.add(episode);
                i++;
            }
            episodesOfSeason.close();
        }

        return new Result(episodes, requestedEpisodeIndex);
    }
}
