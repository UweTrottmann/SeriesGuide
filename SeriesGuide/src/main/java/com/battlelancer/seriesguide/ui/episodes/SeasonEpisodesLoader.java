package com.battlelancer.seriesguide.ui.episodes;

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
class SeasonEpisodesLoader extends GenericSimpleLoader<SeasonEpisodesLoader.Result> {

    private final int seasonTvdbId;
    private final int episodeTvdbId;

    static class Result {
        @NonNull
        public ArrayList<Episode> episodes;
        public int requestedEpisodeIndex;

        public Result(@NonNull ArrayList<Episode> episodes, int requestedEpisodeIndex) {
            this.episodes = episodes;
            this.requestedEpisodeIndex = requestedEpisodeIndex;
        }
    }

    SeasonEpisodesLoader(Context context, int seasonTvdbId, int episodeTvdbId) {
        super(context);
        this.seasonTvdbId = seasonTvdbId;
        this.episodeTvdbId = episodeTvdbId;
    }

    @Override
    public Result loadInBackground() {
        // get episodes of season
        Constants.EpisodeSorting sortOrder = DisplaySettings.getEpisodeSortOrder(getContext());
        Cursor episodesOfSeason = getContext().getContentResolver().query(
                Episodes.buildEpisodesOfSeasonUri(String.valueOf(seasonTvdbId)), new String[] {
                        Episodes._ID, Episodes.NUMBER, Episodes.SEASON
                }, null, null, sortOrder.query()
        );

        ArrayList<Episode> episodes = new ArrayList<>();
        int requestedEpisodeIndex = 0;
        if (episodesOfSeason != null) {
            int i = 0;
            Integer seasonNumber = null;
            while (episodesOfSeason.moveToNext()) {
                int curEpisodeId = episodesOfSeason.getInt(0);
                if (curEpisodeId == episodeTvdbId) {
                    requestedEpisodeIndex = i;
                }
                Episode episode = new Episode();
                episode.episodeId = curEpisodeId;
                episode.episodeNumber = episodesOfSeason.getInt(1);
                if (seasonNumber == null) {
                    seasonNumber = episodesOfSeason.getInt(2); // same for all
                }
                episode.seasonNumber = seasonNumber;
                episodes.add(episode);
                i++;
            }
            episodesOfSeason.close();
        }

        return new Result(episodes, requestedEpisodeIndex);
    }
}
