package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables.EPISODES;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import com.battlelancer.seriesguide.model.SgEpisode;

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

}
