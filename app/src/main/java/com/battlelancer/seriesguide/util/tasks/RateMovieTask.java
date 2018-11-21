package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.enums.Rating;
import org.greenrobot.eventbus.EventBus;

public class RateMovieTask extends BaseRateItemTask {

    private final int movieTmdbId;

    /**
     * Stores the rating for the given movie in the database (if it is in the database) and sends it
     * to trakt.
     */
    public RateMovieTask(Context context, Rating rating, int movieTmdbId) {
        super(context, rating);
        this.movieTmdbId = movieTmdbId;
    }

    @NonNull
    @Override
    protected String getTraktAction() {
        return "rate movie";
    }

    @Nullable
    @Override
    protected SyncItems buildTraktSyncItems() {
        return new SyncItems()
                .movies(new SyncMovie().id(MovieIds.tmdb(movieTmdbId)).rating(getRating()));
    }

    @Override
    protected boolean doDatabaseUpdate() {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Movies.RATING_USER, getRating().value);

        int rowsUpdated = getContext().getContentResolver()
                .update(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null);

        return rowsUpdated > 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);

        // post event so movie UI reloads (it is not listening to database changes)
        EventBus.getDefault().post(new MovieTools.MovieChangedEvent(movieTmdbId));
    }
}
