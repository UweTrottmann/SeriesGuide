package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Rating;

public class RateShowTask extends BaseRateItemTask {

    private final int showTvdbId;

    public RateShowTask(Context context, Rating rating, int showTvdbId) {
        super(context, rating);
        this.showTvdbId = showTvdbId;
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "rate show";
    }

    @Nullable
    @Override
    protected SyncItems buildTraktSyncItems() {
        return new SyncItems()
                .shows(new SyncShow().id(ShowIds.tvdb(showTvdbId)).rating(getRating()));
    }

    @Override
    protected boolean doDatabaseUpdate() {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Shows.RATING_USER, getRating().value);

        int rowsUpdated = getContext().getContentResolver()
                .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values, null, null);

        return rowsUpdated > 0;
    }
}
