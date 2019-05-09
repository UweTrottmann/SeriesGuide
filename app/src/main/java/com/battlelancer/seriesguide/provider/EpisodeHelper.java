package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables.EPISODES;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import com.battlelancer.seriesguide.model.EpisodeWithShow;
import com.battlelancer.seriesguide.model.SgEpisode;
import java.util.List;

/**
 * Data Access Object for the episodes table.
 */
@Dao
public interface EpisodeHelper {

    /**
     * For testing: Get the first episode from the table.
     */
    @Query("SELECT * FROM " + EPISODES + " LIMIT 1")
    SgEpisode getEpisode();

    @Query("SELECT episodes._id AS episodeTvdbId, episodetitle, episodenumber, season, episode_firstairedms, watched, episode_collected, series_id AS showTvdbId, seriestitle, network, poster FROM episodes "
            + "LEFT OUTER JOIN series ON episodes.series_id=series._id "
            + "WHERE episode_firstairedms>=:recentThreshold AND episode_firstairedms<:timeThreshold AND series_hidden=0 "
            + "ORDER BY episode_firstairedms ASC,seriestitle COLLATE NOCASE ASC,episodenumber ASC")
    LiveData<List<EpisodeWithShow>> getUpcomingEpisodes(long recentThreshold, long timeThreshold);

}
