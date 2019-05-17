package com.battlelancer.seriesguide.provider;

import static com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables.EPISODES;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.battlelancer.seriesguide.model.EpisodeWithShow;
import com.battlelancer.seriesguide.model.SgEpisode;
import com.battlelancer.seriesguide.model.SgShow;

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

    @RawQuery(observedEntities = {SgEpisode.class, SgShow.class})
    DataSource.Factory<Integer, EpisodeWithShow> getEpisodesWithShow(SupportSQLiteQuery query);
}
