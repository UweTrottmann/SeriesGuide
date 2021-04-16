package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Rating;

public class RateShowTask extends BaseRateItemTask {

    private final long showId;

    public RateShowTask(Context context, Rating rating, long showId) {
        super(context, rating);
        this.showId = showId;
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "rate show";
    }

    @Nullable
    @Override
    protected SyncItems buildTraktSyncItems() {
        int showTmdbIdOrZero = SgRoomDatabase.getInstance(getContext()).sgShow2Helper()
                .getShowTmdbId(showId);
        if (showTmdbIdOrZero == 0) return null;
        return new SyncItems()
                .shows(new SyncShow().id(ShowIds.tmdb(showTmdbIdOrZero)).rating(getRating()));
    }

    @Override
    protected boolean doDatabaseUpdate() {
        int rowsUpdated = SgRoomDatabase.getInstance(getContext()).sgShow2Helper()
                .updateUserRating(showId, getRating().value);
        return rowsUpdated > 0;
    }
}
