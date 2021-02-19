package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SgEpisode2Numbers;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Rating;

public class RateEpisodeTask extends BaseRateItemTask {

    private final long episodeId;

    public RateEpisodeTask(Context context, Rating rating, long episodeId) {
        super(context, rating);
        this.episodeId = episodeId;
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "rate episode";
    }

    @Nullable
    @Override
    protected SyncItems buildTraktSyncItems() {
        SgRoomDatabase database = SgRoomDatabase.getInstance(getContext());

        SgEpisode2Numbers episode = database.sgEpisode2Helper().getEpisodeNumbers(episodeId);
        if (episode == null) return null;

        int showTmdbId = database.sgShow2Helper().getShowTmdbId(episode.getShowId());
        if (showTmdbId == 0) return null;

        return new SyncItems()
                .shows(new SyncShow().id(ShowIds.tmdb(showTmdbId))
                        .seasons(new SyncSeason().number(episode.getSeason())
                                .episodes(new SyncEpisode().number(episode.getEpisodenumber())
                                        .rating(getRating()))));
    }

    @Override
    protected boolean doDatabaseUpdate() {
        int rowsUpdated = SgRoomDatabase.getInstance(getContext()).sgEpisode2Helper()
                .updateUserRating(episodeId, getRating().value);
        return rowsUpdated > 0;
    }
}
