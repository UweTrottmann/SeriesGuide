package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Rating;

public class RateEpisodeTask extends BaseRateItemTask {

    private final int episodeTvdbId;

    public RateEpisodeTask(Context context, Rating rating, int episodeTvdbId) {
        super(context, rating);
        this.episodeTvdbId = episodeTvdbId;
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "rate episode";
    }

    @Nullable
    @Override
    protected SyncItems buildTraktSyncItems() {
        int season = -1;
        int episode = -1;
        int showTvdbId = -1;
        Cursor query = getContext().getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId),
                        new String[] {
                                SeriesGuideContract.Episodes.SEASON,
                                SeriesGuideContract.Episodes.NUMBER,
                                SeriesGuideContract.Shows.REF_SHOW_ID }, null, null, null);
        if (query != null) {
            if (query.moveToFirst()) {
                season = query.getInt(0);
                episode = query.getInt(1);
                showTvdbId = query.getInt(2);
            }
            query.close();
        }

        if (season == -1 || episode == -1 || showTvdbId == -1) {
            return null;
        }

        return new SyncItems()
                .shows(new SyncShow().id(ShowIds.tvdb(showTvdbId))
                        .seasons(new SyncSeason().number(season)
                                .episodes(new SyncEpisode().number(episode)
                                        .rating(getRating()))));
    }

    @Override
    protected boolean doDatabaseUpdate() {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Episodes.RATING_USER, getRating().value);

        int rowsUpdated = getContext().getContentResolver()
                .update(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), values, null,
                        null);

        // notify withshow uri as well (used by episode details view)
        getContext().getContentResolver()
                .notifyChange(SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                        null);

        return rowsUpdated > 0;
    }
}
