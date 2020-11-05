package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables.EPISODES;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;
import com.battlelancer.seriesguide.model.SgEpisode;
import com.battlelancer.seriesguide.model.SgEpisodeSeasonAndShow;

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

    @Nullable
    @Query("SELECT season_id, season, series_id  FROM episodes WHERE _id=:episodeTvdbId")
    SgEpisodeSeasonAndShow getEpisodeMinimal(int episodeTvdbId);

}
